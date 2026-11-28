package com.example.consolestore.Service;

import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Repository.HoaDonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PaymentGatewayService {

    // Khóa ký link thanh toán (chỉ dùng cho demo, nhớ đổi)
    private static final String SECRET = "doi-chuoi-bi-mat-nay";

    public static final String OTP_OK = "123456";
    public static final String OTP_NO_FUNDS = "111111";

    public enum Result { SUCCESS, WRONG_OTP, NO_FUNDS, EXPIRED, INVALID }
    public enum CancelResult { CANCELLED, ALREADY_PAID, CLOSED, INVALID }
    public record View(
            Long invoiceId,
            String method,
            BigDecimal amount,
            String paymentStatus,
            String orderStatus,
            LocalDateTime createdAt
    ) {
        public boolean isPaid() {
            return "DA_THANH_TOAN".equals(paymentStatus);
        }

        public boolean isClosed() {
            return "HUY".equals(orderStatus);
        }
    }

    private final HoaDonRepository invoices;

    public PaymentGatewayService(HoaDonRepository invoices) {
        this.invoices = invoices;
    }

    // ===== Ký token cho link thanh toán =====

    public static String token(Long invoiceId, BigDecimal amount) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    SECRET.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            String data = invoiceId + "|" + amount.toBigInteger();

            return HexFormat.of()
                    .formatHex(mac.doFinal(
                            data.getBytes(StandardCharsets.UTF_8)
                    ))
                    .substring(0, 32);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String payUrl(Long invoiceId, BigDecimal amount) {
        return "/sandbox/pay/" + invoiceId
                + "?t=" + token(invoiceId, amount);
    }

    // ===== Đọc thông tin giao dịch (null nếu token sai) =====

    @Transactional(readOnly = true)
    public View view(Long invoiceId, String token) {
        HoaDon order = invoices.findById(invoiceId)
                .filter(o -> "ONLINE".equals(o.getChannel()))
                .orElse(null);

        if (order == null
                || token == null
                || !MessageDigest.isEqual(
                token(order.getId(), order.getTotalAmount())
                        .getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        )) {
            return null;
        }

        var pay = invoices.payment(invoiceId).orElse(null);

        if (pay == null || "TIEN_MAT".equals(pay.getMethod())) {
            return null;
        }

        return new View(
                order.getId(),
                pay.getMethod(),
                pay.getAmount(),
                pay.getStatus(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    // ===== Xử lý nhập OTP =====

    @Transactional(timeout = 20)
    public Result pay(Long invoiceId, String token, String otp) {
        View v = view(invoiceId, token);

        if (v == null) {
            return Result.INVALID;
        }

        if (v.isPaid()) {
            return Result.SUCCESS;
        }

        if (v.isClosed()
                || !"CHO_XAC_NHAN".equals(v.orderStatus())) {
            return Result.EXPIRED;
        }

        String code = otp == null ? "" : otp.strip();

        if (OTP_NO_FUNDS.equals(code)) {
            return Result.NO_FUNDS;
        }

        if (!OTP_OK.equals(code)) {
            return Result.WRONG_OTP;
        }

        // Khóa đơn để tránh bấm 2 lần
        invoices.lockOnlineOrder(invoiceId);

        if (invoices.markOnlinePaid(invoiceId) == 1) {
            invoices.autoConfirmOnline(invoiceId);
            return Result.SUCCESS;
        }

        var current = invoices.payment(invoiceId).orElse(null);

        return current != null
                && "DA_THANH_TOAN".equals(current.getStatus())
                ? Result.SUCCESS
                : Result.EXPIRED;
    }
    // ===== Khách hủy giao dịch: hủy luôn đơn nếu chưa thanh toán =====

    @Transactional(timeout = 20)
    public CancelResult cancel(Long invoiceId, String token) {
        View v = view(invoiceId, token);

        if (v == null) {
            return CancelResult.INVALID;
        }

        if (v.isPaid()) {
            return CancelResult.ALREADY_PAID;
        }

        if (v.isClosed()) {
            return CancelResult.CLOSED;
        }

        // Khóa đơn để không đua với lúc quét OTP thành công.
        invoices.lockOnlineOrder(invoiceId);

        if (invoices.cancelUnpaidOnlineOrder(invoiceId) == 1) {
            return CancelResult.CANCELLED;
        }

        var current = invoices.payment(invoiceId).orElse(null);

        return current != null
                && "DA_THANH_TOAN".equals(current.getStatus())
                ? CancelResult.ALREADY_PAID
                : CancelResult.CLOSED;
    }
}