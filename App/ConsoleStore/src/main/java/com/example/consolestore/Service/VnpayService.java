package com.example.consolestore.Service;

import com.example.consolestore.Repository.HoaDonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tích hợp cổng thanh toán VNPAY (môi trường sandbox) cho phương thức
 * CHUYEN_KHOAN. Chỉ bật khi đã cấu hình store.vnpay.tmn-code và
 * store.vnpay.hash-secret; nếu chưa, hệ thống dùng cổng PayLab cũ.
 */
@Service
public class VnpayService {

    private static final Logger log =
            LoggerFactory.getLogger(VnpayService.class);

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern TXN_REF =
            Pattern.compile("^HD(\\d{1,18})T\\d{14}$");
    public static final String REFUND_MARKER = "[CẦN HOÀN TIỀN]";
    public enum Outcome {
        SUCCESS,
        ALREADY_PAID,
        FAILED,
        CANCELLED,
        EXPIRED,
        INVALID_SIGNATURE,
        ORDER_NOT_FOUND,
        AMOUNT_MISMATCH
    }

    public record Result(Outcome outcome, Long invoiceId) {
    }

    private final HoaDonRepository invoices;
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final int timeoutMinutes;

    public VnpayService(
            HoaDonRepository invoices,
            @Value("${store.vnpay.tmn-code:}") String tmnCode,
            @Value("${store.vnpay.hash-secret:}") String hashSecret,
            @Value("${store.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
            String payUrl,
            @Value("${store.vnpay.return-url:http://localhost:8080/vnpay/return}")
            String returnUrl,
            @Value("${store.order-expiry.timeout-minutes:15}")
            int timeoutMinutes
    ) {
        this.invoices = invoices;
        this.tmnCode = tmnCode == null ? "" : tmnCode.strip();
        this.hashSecret = hashSecret == null ? "" : hashSecret.strip();
        this.payUrl = payUrl.strip();
        this.returnUrl = returnUrl.strip();
        this.timeoutMinutes = timeoutMinutes;
    }

    public boolean enabled() {
        return !tmnCode.isEmpty() && !hashSecret.isEmpty();
    }

    /**
     * Tạo URL sang trang thanh toán VNPAY.
     * Trả về null nếu đơn đã hết hạn thanh toán.
     */
    public String buildPayUrl(
            Long invoiceId,
            BigDecimal amount,
            LocalDateTime orderCreatedAt,
            String clientIp
    ) {
        Duration remaining = Duration.between(
                LocalDateTime.now(),
                orderCreatedAt.plusMinutes(timeoutMinutes)
        );

        // Còn dưới 1 phút thì coi như hết hạn
        if (remaining.getSeconds() < 60) {
            return null;
        }

        ZonedDateTime now = ZonedDateTime.now(VN);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        // VNPAY yêu cầu số tiền nhân 100
        params.put("vnp_Amount", amount.movePointRight(2)
                .toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        // Mã tham chiếu duy nhất cho từng lần thanh toán (cho phép thử lại)
        params.put("vnp_TxnRef", "HD" + invoiceId + "T" + STAMP.format(now));
        params.put("vnp_OrderInfo", "Thanh toan don hang HD" + invoiceId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", STAMP.format(now));
        // Hết hạn cùng lúc với đơn hàng
        params.put("vnp_ExpireDate",
                STAMP.format(now.plus(remaining)));

        return VnpaySigner.paymentUrl(payUrl, hashSecret, params);
    }

    /**
     * Xử lý kết quả VNPAY (dùng chung cho return URL và IPN).
     * Idempotent: gọi nhiều lần vẫn chỉ ghi nhận thanh toán một lần.
     */
    @Transactional(timeout = 20)
    public Result handle(Map<String, String> params) {
        if (!enabled() || !VnpaySigner.verify(params, hashSecret)) {
            return new Result(Outcome.INVALID_SIGNATURE, null);
        }

        if (!tmnCode.equals(params.get("vnp_TmnCode"))) {
            return new Result(Outcome.INVALID_SIGNATURE, null);
        }

        Matcher ref = TXN_REF.matcher(
                params.getOrDefault("vnp_TxnRef", "")
        );

        if (!ref.matches()) {
            return new Result(Outcome.ORDER_NOT_FOUND, null);
        }

        Long invoiceId = Long.valueOf(ref.group(1));

        var order = invoices.findById(invoiceId)
                .filter(o -> "ONLINE".equals(o.getChannel()))
                .orElse(null);

        var payment = order == null
                ? null
                : invoices.payment(invoiceId).orElse(null);

        if (payment == null
                || !"CHUYEN_KHOAN".equals(payment.getMethod())) {
            return new Result(Outcome.ORDER_NOT_FOUND, invoiceId);
        }

        // Đối chiếu số tiền VNPAY báo về với số tiền đã lưu trong hệ thống
        BigDecimal reported;

        try {
            reported = new BigDecimal(
                    params.getOrDefault("vnp_Amount", "")
            );
        } catch (NumberFormatException exception) {
            return new Result(Outcome.AMOUNT_MISMATCH, invoiceId);
        }

        if (reported.compareTo(payment.getAmount().movePointRight(2)) != 0) {
            return new Result(Outcome.AMOUNT_MISMATCH, invoiceId);
        }

        if ("DA_THANH_TOAN".equals(payment.getStatus())) {
            return new Result(Outcome.ALREADY_PAID, invoiceId);
        }

        boolean paid = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));

        if (!paid) {
            // 24 = khách chủ động hủy giao dịch tại VNPAY
            return new Result(
                    "24".equals(params.get("vnp_ResponseCode"))
                            ? Outcome.CANCELLED
                            : Outcome.FAILED,
                    invoiceId
            );
        }

        // Khóa đơn để return URL và IPN không ghi nhận trùng
        invoices.lockOnlineOrder(invoiceId);

        if (invoices.markOnlinePaid(invoiceId) == 1) {
            invoices.autoConfirmOnline(invoiceId);

            String transactionNo = params.getOrDefault(
                    "vnp_TransactionNo", ""
            ).replaceAll("[^0-9A-Za-z]", "");

            if (!transactionNo.isEmpty()) {
                invoices.saveTransactionCode(
                        invoiceId,
                        "VNPAY" + transactionNo
                );
            }

            return new Result(Outcome.SUCCESS, invoiceId);
        }

        var current = invoices.payment(invoiceId).orElse(null);

        if (current != null
                && "DA_THANH_TOAN".equals(current.getStatus())) {
            return new Result(Outcome.ALREADY_PAID, invoiceId);
        }

        // VNPAY đã trừ tiền nhưng đơn đã bị hủy/hết hạn: ghi dấu vết để nhân viên hoàn tiền
        String refundTxn = params.getOrDefault("vnp_TransactionNo", "")
                .replaceAll("[^0-9A-Za-z]", "");

        String refundNote = REFUND_MARKER + " Khách đã trả "
                + payment.getAmount().toPlainString()
                + "đ qua VNPAY (mã GD "
                + (refundTxn.isEmpty() ? "không rõ" : refundTxn)
                + ") nhưng đơn đã hủy/hết hạn.";

        invoices.markRefundNeeded(invoiceId, refundNote, REFUND_MARKER);

        log.warn("VNPAY paid but order HD{} is no longer open, txn={}",
                invoiceId, refundTxn);

        return new Result(Outcome.EXPIRED, invoiceId);
    }

    /** Lấy các tham số vnp_* từ request (đã giải mã). */
    public static Map<String, String> vnpParams(
            Map<String, String[]> raw
    ) {
        Map<String, String> params = new TreeMap<>();

        raw.forEach((key, values) -> {
            if (key.startsWith("vnp_") && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        return params;
    }
}
