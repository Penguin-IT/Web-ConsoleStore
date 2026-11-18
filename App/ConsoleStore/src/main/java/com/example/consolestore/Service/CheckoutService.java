package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

@Service
public class CheckoutService {

    private static final BigDecimal MAX_AMOUNT =
            new BigDecimal("999999999999999999");

    private static final Set<String> METHODS = Set.of(
            "CHUYEN_KHOAN",
            "MOMO",
            "ZALOPAY"
    );

    private final TaiKhoanRepository accounts;
    private final SanPhamRepository products;
    private final TonKhoRepository stocks;
    private final HoaDonRepository invoices;
    private final TransactionTemplate transaction;

    public CheckoutService(
            TaiKhoanRepository accounts,
            SanPhamRepository products,
            TonKhoRepository stocks,
            HoaDonRepository invoices,
            PlatformTransactionManager manager
    ) {
        this.accounts = accounts;
        this.products = products;
        this.stocks = stocks;
        this.invoices = invoices;

        transaction = new TransactionTemplate(manager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        transaction.setTimeout(20);
    }

    @Transactional(readOnly = true)
    public CheckoutRequest defaults(String username) {
        KhachHang customer = customer(username);

        CheckoutRequest form = new CheckoutRequest();
        form.setFullName(customer.getFullName());
        form.setPhone(customer.getPhone());
        form.setAddress(customer.getAddress());

        return form;
    }

    @Transactional(readOnly = true)
    public Optional<Long> findExisting(
            String username,
            String requestId
    ) {
        Long customerId = customer(username).getId();

        return invoices.findByRequestIdAndCustomerId(
                requestId,
                customerId
        ).map(HoaDon::getId);
    }

    public Long place(
            String username,
            CheckoutQuote quote,
            CheckoutRequest form
    ) {
        if (quote == null || quote.items().isEmpty()) {
            throw rejected("Giỏ hàng đang trống.");
        }

        for (int attempt = 0; ; attempt++) {
            try {
                return Objects.requireNonNull(
                        transaction.execute(
                                status -> create(
                                        username,
                                        quote,
                                        form
                                )
                        )
                );
            } catch (RuntimeException exception) {
                if (attempt >= 2 || !isDeadlock(exception)) {
                    throw exception;
                }
            }
        }
    }

    private Long create(
            String username,
            CheckoutQuote quote,
            CheckoutRequest form
    ) {
        Long customerId = customer(username).getId();

        Optional<HoaDon> existing =
                invoices.lockByRequestId(quote.requestId());

        if (existing.isPresent()) {
            if (!customerId.equals(
                    existing.get().getCustomerId()
            )) {
                throw new AccessDeniedException(
                        "Request owner mismatch"
                );
            }

            return existing.get().getId();
        }

        String name = required(
                form.getFullName(),
                100,
                "Họ tên"
        );

        String phone = required(
                form.getPhone(),
                10,
                "Số điện thoại"
        );

        if (!phone.matches("0[0-9]{9}")) {
            throw rejected(
                    "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0."
            );
        }

        String address = required(
                form.getAddress(),
                255,
                "Địa chỉ"
        );

        String note = clean(form.getNote());

        if (note.length() > 255) {
            throw rejected("Ghi chú tối đa 255 ký tự.");
        }

        String method = clean(form.getPaymentMethod());

        if (!METHODS.contains(method)) {
            throw rejected(
                    "Phương thức thanh toán không hợp lệ."
            );
        }

        if (!quote.prices().keySet().equals(
                quote.items().keySet()
        )) {
            throw rejected(
                    "Thông tin giỏ hàng đã thay đổi. "
                            + "Vui lòng kiểm tra lại."
            );
        }

        List<SanPham> found = products.lockForCheckout(
                quote.items().keySet()
        );

        Map<Long, SanPham> byId = new TreeMap<>();

        found.forEach(
                product -> byId.put(
                        product.getMaSP(),
                        product
                )
        );

        if (byId.size() != quote.items().size()) {
            throw rejected(
                    "Có sản phẩm đã ngừng bán. "
                            + "Vui lòng kiểm tra giỏ hàng."
            );
        }

        BigDecimal total = BigDecimal.ZERO;

        for (var entry : quote.items().entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || entry.getValue() <= 0) {
                throw rejected(
                        "Số lượng sản phẩm không hợp lệ."
                );
            }

            SanPham product = byId.get(entry.getKey());

            BigDecimal price = product.getGiaBan();
            BigDecimal quotedPrice =
                    quote.prices().get(entry.getKey());

            if (price == null
                    || price.signum() <= 0
                    || price.scale() > 0
                    || quotedPrice == null
                    || price.compareTo(quotedPrice) != 0) {
                throw rejected(
                        "Giá sản phẩm đã thay đổi. "
                                + "Vui lòng xem lại trước khi đặt."
                );
            }

            total = total.add(
                    price.multiply(
                            BigDecimal.valueOf(
                                    entry.getValue()
                            )
                    )
            );
        }

        if (total.signum() <= 0
                || total.compareTo(MAX_AMOUNT) > 0) {
            throw rejected("Tổng tiền không hợp lệ.");
        }

        List<StockAllocator.Allocation> allocations;

        try {
            allocations = StockAllocator.allocate(
                    quote.items(),
                    stocks.lockForCheckout(
                                    quote.items().keySet()
                            )
                            .stream()
                            .map(row -> new StockAllocator.Stock(
                                    row.getWarehouseId(),
                                    row.getProductId(),
                                    row.getQuantity()
                            ))
                            .toList()
            );
        } catch (IllegalArgumentException exception) {
            throw rejected(exception.getMessage());
        }

        HoaDon invoice = invoices.saveAndFlush(
                new HoaDon(
                        customerId,
                        quote.requestId(),
                        name,
                        phone,
                        address,
                        note
                )
        );

        for (var allocation : allocations) {
            SanPham product = byId.get(
                    allocation.productId()
            );

            boolean console = "Máy console".equals(
                    product.getLoaiSanPham().getName()
            );

            int remaining = allocation.quantity();

            while (remaining > 0) {
                // Mỗi máy một dòng để gán một serial ở bước xử lý đơn.
                int quantity = console ? 1 : remaining;

                invoices.insertLine(
                        invoice.getId(),
                        allocation.productId(),
                        allocation.warehouseId(),
                        quantity,
                        product.getGiaBan()
                );

                remaining -= quantity;
            }
        }

        BigDecimal storedTotal = invoices.readTotal(
                invoice.getId()
        );

        if (storedTotal == null
                || storedTotal.compareTo(total) != 0) {
            throw new IllegalStateException(
                    "Invoice total does not match line total"
            );
        }

        invoices.insertPayment(
                invoice.getId(),
                method,
                total
        );

        return invoice.getId();
    }

    @Transactional(readOnly = true)
    public PaymentPage payment(
            String username,
            Long invoiceId
    ) {
        Long customerId = customer(username).getId();

        HoaDon invoice = invoices.findByIdAndCustomerId(
                invoiceId,
                customerId
        ).orElseThrow(
                () -> new org.springframework.web.server
                        .ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND
                )
        );

        HoaDonRepository.PaymentInfo payment =
                invoices.payment(invoiceId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment is missing"
                                )
                        );

        return new PaymentPage(
                invoice,
                payment,
                invoices.lines(invoiceId)
        );
    }

    private KhachHang customer(String username) {
        TaiKhoan account = accounts.findWithCustomer(
                username
        ).orElseThrow(
                () -> new AccessDeniedException(
                        "Account is missing"
                )
        );

        if (account.isLocked()
                || !"KHACHHANG".equals(account.getRole())
                || account.getCustomer() == null) {
            throw new AccessDeniedException(
                    "Customer access required"
            );
        }

        return account.getCustomer();
    }

    private static String clean(String text) {
        return text == null ? "" : text.strip();
    }

    private static String required(
            String value,
            int max,
            String label
    ) {
        String text = clean(value);

        if (text.isEmpty() || text.length() > max) {
            throw rejected(
                    label + " bắt buộc, tối đa "
                            + max + " ký tự."
            );
        }

        return text;
    }

    private static CheckoutRejectedException rejected(
            String message
    ) {
        return new CheckoutRejectedException(message);
    }

    private static boolean isDeadlock(Throwable exception) {
        for (
                Throwable current = exception;
                current != null;
                current = current.getCause()
        ) {
            if (current instanceof SQLException sql
                    && sql.getErrorCode() == 1205) {
                return true;
            }
        }

        return false;
    }
}