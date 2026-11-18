package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PaymentApprovalService {

    private static final Set<String> METHODS = Set.of(
            "CHUYEN_KHOAN",
            "MOMO",
            "ZALOPAY"
    );

    private final ThanhToanRepository payments;
    private final HoaDonRepository invoices;
    private final TaiKhoanRepository accounts;

    public PaymentApprovalService(
            ThanhToanRepository payments,
            HoaDonRepository invoices,
            TaiKhoanRepository accounts
    ) {
        this.payments = payments;
        this.invoices = invoices;
        this.accounts = accounts;
    }

    public Page<ThanhToanRepository.PendingRow> list(
            String username,
            String keyword,
            String method,
            int page
    ) {
        admin(username);

        String query = clean(keyword);
        String selected = clean(method);

        if (query.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Từ khóa tối đa 100 ký tự."
            );
        }

        if (!selected.isEmpty()
                && !METHODS.contains(selected)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức không hợp lệ."
            );
        }

        query = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return payments.pending(
                query,
                selected,
                PageRequest.of(Math.max(0, page), 20)
        );
    }

    public PaymentReview detail(
            String username,
            Long id
    ) {
        admin(username);

        var payment = payments.findById(id)
                .orElseThrow(PaymentApprovalService::notFound);

        var invoice = invoices.findById(
                payment.getInvoiceId()
        ).filter(order ->
                "ONLINE".equals(order.getChannel())
        ).orElseThrow(PaymentApprovalService::notFound);

        return new PaymentReview(
                payment,
                invoice,
                invoices.lines(invoice.getId())
        );
    }

    @Transactional(timeout = 20)
    public void review(
            String username,
            Long id,
            PaymentReviewRequest form
    ) {
        var actor = admin(username);

        String action = clean(form.getAction());

        if (!Set.of("approve", "reject").contains(action)) {
            throw bad("Thao tác không hợp lệ.");
        }

        Long invoiceId = payments.invoiceId(id)
                .orElseThrow(PaymentApprovalService::notFound);

        var invoice = invoices.lockOnlineOrder(invoiceId)
                .orElseThrow(PaymentApprovalService::notFound);

        var payment = payments.lockById(id)
                .orElseThrow(PaymentApprovalService::notFound);

        if (!invoiceId.equals(payment.getInvoiceId())
                || !METHODS.contains(payment.getMethod())) {
            throw bad(
                    "Giao dịch không phù hợp để duyệt tại đây."
            );
        }

        boolean paid = "DA_THANH_TOAN".equals(
                payment.getStatus()
        );

        if ("approve".equals(action)
                && paid
                && Set.of(
                "DA_XAC_NHAN",
                "DANG_GIAO",
                "HOAN_TAT"
        ).contains(invoice.getStatus())) {
            return;
        }

        if ("reject".equals(action)
                && "HUY".equals(invoice.getStatus())
                && !paid) {
            return;
        }

        if (!"CHO_XAC_NHAN".equals(invoice.getStatus())) {
            throw bad(
                    "Đơn không còn ở trạng thái chờ xác nhận."
            );
        }

        if (!"CHUA_THANH_TOAN".equals(payment.getStatus())
                && !paid) {
            throw bad("Trạng thái thanh toán không hợp lệ.");
        }

        if (!Objects.equals(
                form.getExpectedMethod(),
                payment.getMethod()
        ) || money(form.getExpectedAmount()).compareTo(
                payment.getAmount()
        ) != 0) {
            throw bad(
                    "Thông tin giao dịch đã thay đổi. Vui lòng tải lại."
            );
        }

        if ("reject".equals(action)) {
            if (paid) {
                throw bad(
                        "Không thể từ chối giao dịch "
                                + "đã xác nhận nhận tiền."
                );
            }

            invoices.reviewOnlinePayment(
                    invoiceId,
                    "HUY",
                    actor.getEmployeeId()
            );

            if (!"HUY".equals(
                    invoices.onlineStatus(invoiceId)
            )) {
                throw bad("Chưa hủy được đơn.");
            }

            return;
        }

        if (!form.isReceived()) {
            throw bad(
                    "Cần xác nhận đã kiểm tra và nhận đủ tiền."
            );
        }

        BigDecimal received = money(
                form.getReceivedAmount()
        );

        if (invoice.getTotalAmount().signum() <= 0
                || received.compareTo(
                payment.getAmount()
        ) != 0
                || received.compareTo(
                invoice.getTotalAmount()
        ) != 0) {
            throw bad(
                    "Số tiền thực nhận phải bằng số tiền "
                            + "thanh toán và tổng hóa đơn."
            );
        }

        var lines = invoices.lines(invoiceId);

        BigDecimal total = lines.stream()
                .map(HoaDonRepository.LineInfo::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        if (lines.isEmpty()
                || total.compareTo(
                invoice.getTotalAmount()
        ) != 0) {
            throw bad(
                    "Chi tiết và tổng tiền hóa đơn không khớp."
            );
        }

        if (!paid) {
            payments.approve(id);
        }

        var state = payments.state(id)
                .orElseThrow(PaymentApprovalService::notFound);

        if (!"DA_THANH_TOAN".equals(state.getStatus())
                || state.getPaidAt() == null
                || state.getAmount().compareTo(
                invoice.getTotalAmount()
        ) != 0) {
            throw bad("Chưa ghi nhận được thanh toán.");
        }

        invoices.reviewOnlinePayment(
                invoiceId,
                "DA_XAC_NHAN",
                actor.getEmployeeId()
        );

        if (!"DA_XAC_NHAN".equals(
                invoices.onlineStatus(invoiceId)
        )) {
            throw bad("Chưa xác nhận được đơn hàng.");
        }
    }

    private TaiKhoanRepository.PaymentAdmin admin(
            String username
    ) {
        return accounts.paymentAdmin(username)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Admin access required"
                        )
                );
    }

    private static BigDecimal money(String value) {
        String text = clean(value);

        if (!text.matches("[0-9]{1,18}")) {
            throw bad(
                    "Nhập số tiền bằng chữ số, tối đa 18 chữ số."
            );
        }

        return new BigDecimal(text);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND
        );
    }
}