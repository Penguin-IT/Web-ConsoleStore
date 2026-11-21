package com.example.consolestore.Service;

import com.example.consolestore.DTO.OrderDetail;
import com.example.consolestore.DTO.OrderSummary;
import com.example.consolestore.Entity.TaiKhoan;
import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final TaiKhoanRepository accounts;
    private final HoaDonRepository invoices;

    public OrderService(
            TaiKhoanRepository accounts,
            HoaDonRepository invoices
    ) {
        this.accounts = accounts;
        this.invoices = invoices;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> list(
            String username,
            int page
    ) {
        Long customerId = customerId(username);

        return invoices.findCustomerOrders(
                customerId,
                PageRequest.of(Math.max(page, 0), 10)
        ).map(row -> new OrderSummary(
                row.getId(),
                row.getCreatedAt(),
                row.getChannel(),
                row.getStatus(),
                row.getTotalAmount(),
                row.getPaymentStatus(),
                row.getPaymentMethod()
        ));
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(
            String username,
            Long id
    ) {
        Long customerId = customerId(username);

        HoaDonRepository.CustomerOrderHeader header =
                invoices.findCustomerOrderHeader(id, customerId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));

        var lines = invoices.findCustomerOrderLines(id, customerId);

        return new OrderDetail(header, lines);
    }

    @Transactional(timeout = 20)
    public void cancel(
            String username,
            Long invoiceId
    ) {
        Long customerId = customerId(username);

        HoaDonRepository.OrderState invoice =
                invoices.lockCustomerOrder(
                        invoiceId,
                        customerId
                ).orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.")
                );

        if (!"ONLINE".equals(invoice.getChannel())) {
            throw new IllegalArgumentException("Không thể hủy đơn tại quầy từ trang này.");
        }

        // Hủy lặp không cập nhật hóa đơn và không hoàn tồn lần nữa.
        if ("HUY".equals(invoice.getStatus())) {
            return;
        }

        if (!"CHO_XAC_NHAN".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("Chỉ được hủy đơn online đang chờ xác nhận.");
        }

        var payment = invoices.lockOrderPayment(invoiceId);
        if (payment.isPresent() && !"CHUA_THANH_TOAN".equals(payment.get().getStatus())) {
            throw new IllegalArgumentException("Đơn đã thanh toán, không thể hủy theo cách này.");
        }

        int updated = invoices.cancelUnpaidOrder(
                invoiceId,
                customerId
        );

        if (updated != 1) {
            throw new IllegalArgumentException(
                    "Trạng thái đơn vừa thay đổi. "
                            + "Vui lòng tải lại.");
        }


    }

    private Long customerId(String username) {
        TaiKhoan account = accounts.findWithCustomer(
                username
        ).orElseThrow(
                () -> new AccessDeniedException("Account is missing")
        );

        if (account.isLocked()
                || !"KHACHHANG".equals(account.getRole())
                || account.getCustomer() == null) {
            throw new AccessDeniedException("Customer access required");
        }

        return account.getCustomer().getId();
    }
}
