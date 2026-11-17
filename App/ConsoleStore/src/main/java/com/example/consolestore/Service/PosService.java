package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.DTO.PosData.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PosService {

    private static final BigDecimal MAX =
            new BigDecimal("999999999999999999");

    private final TaiKhoanRepository accounts;
    private final KhachHangRepository customers;
    private final SanPhamRepository products;
    private final TonKhoRepository stocks;
    private final SerialMayRepository serials;
    private final KhoRepository warehouses;
    private final PhieuThuCuRepository trades;
    private final HoaDonRepository invoices;
    private final TransactionTemplate transaction;

    public PosService(
            TaiKhoanRepository accounts,
            KhachHangRepository customers,
            SanPhamRepository products,
            TonKhoRepository stocks,
            SerialMayRepository serials,
            KhoRepository warehouses,
            PhieuThuCuRepository trades,
            HoaDonRepository invoices,
            PlatformTransactionManager manager
    ) {
        this.accounts = accounts;
        this.customers = customers;
        this.products = products;
        this.stocks = stocks;
        this.serials = serials;
        this.warehouses = warehouses;
        this.trades = trades;
        this.invoices = invoices;

        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        transaction.setTimeout(20);
    }

    private TaiKhoanRepository.PosStaff staff(String username) {
        return accounts.posStaff(username)
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "Active staff required"
                        )
                );
    }

    @Transactional(readOnly = true)
    public Options options(
            String username,
            Long warehouseId
    ) {
        staff(username);

        List<Kho> all = warehouses.findAll(
                Sort.by("id")
        );

        if (all.isEmpty()) {
            throw bad("Chưa có kho.");
        }

        Long selected = warehouseId == null
                ? all.get(0).getId()
                : warehouseId;

        if (all.stream().noneMatch(
                kho -> kho.getId().equals(selected)
        )) {
            throw bad("Kho không tồn tại.");
        }

        return new Options(
                all,
                selected,
                products.posOptions(selected),
                serials.available(selected)
        );
    }

    @Transactional(readOnly = true)
    public Quote preview(
            String username,
            PosRequest form
    ) {
        var actor = staff(username);

        if (form.getWarehouseId() == null) {
            throw bad("Chọn kho trước khi lập hóa đơn.");
        }

        Kho warehouse = warehouses.findById(
                form.getWarehouseId()
        ).orElseThrow(
                () -> bad("Chọn kho hợp lệ.")
        );

        if (form.getLines() == null
                || form.getLines().isEmpty()
                || form.getLines().size() > 30) {
            throw bad("Hóa đơn cần từ 1 đến 30 dòng.");
        }

        Set<Long> ids = new TreeSet<>();
        Set<Long> serialIds = new TreeSet<>();

        for (var item : form.getLines()) {
            if (item == null
                    || item.getProductId() == null
                    || item.getQuantity() <= 0) {
                throw bad(
                        "Chọn sản phẩm và số lượng dương cho từng dòng."
                );
            }

            ids.add(item.getProductId());

            if (item.getSerialId() != null
                    && !serialIds.add(item.getSerialId())) {
                throw bad(
                        "Một serial không được chọn hai lần."
                );
            }
        }

        Map<Long, SanPham> byId =
                products.findAllById(ids).stream()
                        .collect(Collectors.toMap(
                                SanPham::getMaSP,
                                Function.identity()
                        ));

        Map<Long, SerialMay> bySerial =
                serialIds.isEmpty()
                        ? Map.of()
                        : serials.findAllById(serialIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SerialMay::getId,
                                Function.identity()
                        ));

        List<Line> lines = new ArrayList<>();

        for (var item : form.getLines()) {
            SanPham product = byId.get(
                    item.getProductId()
            );

            if (product == null || product.isDeleted()) {
                throw bad(
                        "Có sản phẩm đã ngừng bán."
                );
            }

            validPrice(product);

            SerialMay serial = item.getSerialId() == null
                    ? null
                    : bySerial.get(item.getSerialId());

            validateSerial(
                    product,
                    item.getSerialId(),
                    item.getQuantity(),
                    serial,
                    warehouse.getId()
            );

            lines.add(new Line(
                    product.getMaSP(),
                    product.getTenSP(),
                    item.getSerialId(),
                    serial == null ? null : serial.getNumber(),
                    item.getQuantity(),
                    product.getGiaBan()
            ));
        }

        Customer contact = contact(form);

        if (form.getTradeInId() != null) {
            validTrade(
                    trades.findById(form.getTradeInId())
                            .orElseThrow(
                                    () -> bad(
                                            "Không tìm thấy phiếu thu cũ."
                                    )
                            ),
                    contact.id()
            );
        }

        String note = clean(form.getNote());

        if (note.length() > 150) {
            throw bad("Ghi chú tối đa 150 ký tự.");
        }

        Quote quote = new Quote(
                UUID.randomUUID().toString(),
                actor.getEmployeeId(),
                warehouse.getId(),
                warehouse.getName(),
                contact,
                form.getTradeInId(),
                note,
                lines
        );

        validTotal(quote.getTotal());

        checkStock(
                quote,
                stocks.posStocks(
                        warehouse.getId(),
                        ids
                )
        );

        return quote;
    }

    private Customer contact(PosRequest form) {
        String phone = clean(form.getPhone());

        if (phone.isEmpty()) {
            if (form.isNewCustomer()
                    || form.getTradeInId() != null) {
                throw bad(
                        "Cần số điện thoại khi thêm khách "
                                + "hoặc liên kết phiếu thu."
                );
            }

            return new Customer(
                    null,
                    "Khách vãng lai",
                    "",
                    "",
                    false
            );
        }

        if (!phone.matches("0[0-9]{9}")) {
            throw bad(
                    "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0."
            );
        }

        Optional<KhachHang> found =
                customers.findByPhone(phone);

        if (found.isPresent()) {
            KhachHang customer = found.get();

            return new Customer(
                    customer.getId(),
                    customer.getFullName(),
                    customer.getPhone(),
                    clean(customer.getAddress()),
                    false
            );
        }

        if (!form.isNewCustomer()) {
            throw bad(
                    "Chưa có khách. Chọn thêm khách mới và nhập họ tên."
            );
        }

        String name = clean(form.getFullName());
        String address = clean(form.getAddress());

        if (name.isEmpty()
                || name.length() > 100
                || address.length() > 255) {
            throw bad(
                    "Họ tên bắt buộc, tối đa 100 ký tự; "
                            + "địa chỉ tối đa 255 ký tự."
            );
        }

        return new Customer(
                null,
                name,
                phone,
                address,
                true
        );
    }

    public Long place(
            String username,
            Quote quote,
            PosPaymentRequest payment
    ) {
        if (quote == null) {
            throw bad("Phiên bán hàng đã hết.");
        }

        for (int attempt = 0; ; attempt++) {
            try {
                return Objects.requireNonNull(
                        transaction.execute(
                                status -> write(
                                        username,
                                        quote,
                                        payment
                                )
                        )
                );
            } catch (RuntimeException exception) {
                if (attempt >= 2 || !deadlock(exception)) {
                    throw exception;
                }
            }
        }
    }

    private Long write(
            String username,
            Quote quote,
            PosPaymentRequest payment
    ) {
        var actor = staff(username);

        if (!actor.getEmployeeId().equals(
                quote.employeeId()
        )) {
            throw new AccessDeniedException(
                    "Draft owner mismatch"
            );
        }

        Optional<HoaDon> old =
                invoices.lockByRequestId(
                        quote.requestId()
                );

        if (old.isPresent()) {
            owned(
                    old.get(),
                    actor.getEmployeeId()
            );

            return old.get().getId();
        }

        String method = clean(payment.getMethod());

        if (!Set.of(
                "TIEN_MAT",
                "CHUYEN_KHOAN",
                "MOMO",
                "ZALOPAY"
        ).contains(method)) {
            throw bad(
                    "Phương thức thanh toán không hợp lệ."
            );
        }

        if (!payment.isConfirmed()) {
            throw bad(
                    "Cần xác nhận đã kiểm tra và nhận đủ tiền."
            );
        }

        BigDecimal total = quote.getTotal();
        validTotal(total);

        BigDecimal received = null;
        BigDecimal change = null;

        if ("TIEN_MAT".equals(method)) {
            String value = clean(
                    payment.getCashReceived()
            );

            if (!value.matches("[0-9]{1,18}")) {
                throw bad(
                        "Nhập tiền khách đưa bằng số nguyên, "
                                + "tối đa 18 chữ số."
                );
            }

            received = new BigDecimal(value);

            if (received.compareTo(total) < 0) {
                throw bad(
                        "Tiền khách đưa chưa đủ."
                );
            }

            change = received.subtract(total);
        } else if (!clean(
                payment.getCashReceived()
        ).isEmpty()) {
            throw bad(
                    "Chỉ nhập tiền khách đưa khi chọn tiền mặt."
            );
        }

        Long customerId = null;
        Customer contact = quote.customer();

        if (!contact.phone().isEmpty()) {
            Optional<KhachHang> found =
                    customers.lockByPhone(
                            contact.phone()
                    );

            if (contact.newCustomer()) {
                if (found.isPresent()) {
                    throw bad(
                            "Số điện thoại vừa được tạo. "
                                    + "Hãy xem lại thông tin khách."
                    );
                }

                KhachHang customer = new KhachHang(
                        contact.name(),
                        contact.phone(),
                        null
                );

                customer.updateProfile(
                        contact.name(),
                        contact.phone(),
                        contact.address()
                );

                customerId = customers.saveAndFlush(
                        customer
                ).getId();
            } else {
                KhachHang customer = found.orElseThrow(
                        () -> bad(
                                "Thông tin khách đã thay đổi."
                        )
                );

                if (!customer.getId().equals(
                        contact.id()
                )) {
                    throw bad(
                            "Thông tin khách đã thay đổi."
                    );
                }

                customerId = customer.getId();
            }
        }

        Set<Long> ids = quote.lines().stream()
                .map(Line::productId)
                .collect(Collectors.toCollection(
                        TreeSet::new
                ));

        Map<Long, SanPham> byId =
                products.lockForCheckout(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                SanPham::getMaSP,
                                Function.identity()
                        ));

        for (var line : quote.lines()) {
            SanPham product = byId.get(
                    line.productId()
            );

            if (product == null || product.isDeleted()) {
                throw bad(
                        "Có sản phẩm đã ngừng bán."
                );
            }

            validPrice(product);

            if (product.getGiaBan().compareTo(
                    line.price()
            ) != 0) {
                throw bad(
                        "Giá " + product.getTenSP()
                                + " đã đổi. Hãy xem lại hóa đơn."
                );
            }
        }

        checkStock(
                quote,
                stocks.lockPosStocks(
                        quote.warehouseId(),
                        ids
                )
        );

        List<Long> selected = quote.lines().stream()
                .map(Line::serialId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (new HashSet<>(selected).size()
                != selected.size()) {
            throw bad("Serial bị chọn trùng.");
        }

        Map<Long, SerialMay> selectedSerials =
                selected.isEmpty()
                        ? Map.of()
                        : serials.lockIds(selected)
                        .stream()
                        .collect(Collectors.toMap(
                                SerialMay::getId,
                                Function.identity()
                        ));

        for (var line : quote.lines()) {
            validateSerial(
                    byId.get(line.productId()),
                    line.serialId(),
                    line.quantity(),
                    line.serialId() == null
                            ? null
                            : selectedSerials.get(
                            line.serialId()
                    ),
                    quote.warehouseId()
            );
        }

        if (quote.tradeInId() != null) {
            validTrade(
                    trades.lockById(quote.tradeInId())
                            .orElseThrow(
                                    () -> bad(
                                            "Phiếu thu không tồn tại."
                                    )
                            ),
                    customerId
            );
        }

        String note = quote.note();

        if (received != null) {
            note += (note.isEmpty() ? "" : " | ")
                    + "Tiền khách đưa: "
                    + received.toPlainString()
                    + "; Tiền thối: "
                    + change.toPlainString();
        }

        if (note.length() > 255) {
            throw bad(
                    "Ghi chú vượt giới hạn."
            );
        }

        HoaDon invoice = invoices.saveAndFlush(
                HoaDon.counter(
                        customerId,
                        actor.getEmployeeId(),
                        quote.warehouseId(),
                        contact.name(),
                        contact.phone().isEmpty()
                                ? null
                                : contact.phone(),
                        note,
                        quote.requestId()
                )
        );

        if (quote.tradeInId() != null
                && trades.attach(
                quote.tradeInId(),
                customerId,
                invoice.getId()
        ) != 1) {
            throw bad(
                    "Phiếu thu vừa được liên kết. "
                            + "Hãy kiểm tra lại."
            );
        }

        List<Line> ordered = quote.lines().stream()
                .sorted(
                        Comparator.comparing(
                                Line::productId
                        ).thenComparing(
                                line -> line.serialId() == null
                                        ? 0L
                                        : line.serialId()
                        )
                )
                .toList();

        for (var line : ordered) {
            invoices.insertPosLine(
                    invoice.getId(),
                    line.productId(),
                    quote.warehouseId(),
                    line.serialId(),
                    line.quantity(),
                    line.price()
            );
        }

        BigDecimal stored = invoices.readTotal(
                invoice.getId()
        );

        if (stored == null
                || stored.compareTo(total) != 0) {
            throw new IllegalStateException(
                    "Invoice total mismatch"
            );
        }

        invoices.insertPosPayment(
                invoice.getId(),
                method,
                total
        );

        if (invoices.finishPos(
                invoice.getId()
        ) != 1) {
            throw new IllegalStateException(
                    "Cannot finish invoice"
            );
        }

        return invoice.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Long> existing(
            String username,
            String requestId
    ) {
        var actor = staff(username);

        return invoices.findByRequestId(
                requestId
        ).map(invoice -> {
            owned(
                    invoice,
                    actor.getEmployeeId()
            );

            return invoice.getId();
        });
    }

    @Transactional(readOnly = true)
    public Receipt receipt(
            String username,
            Long id
    ) {
        var actor = staff(username);

        HoaDon invoice = invoices.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        if (!"QUAY".equals(invoice.getChannel())
                || (
                !"ADMIN".equals(actor.getRole())
                        && !actor.getEmployeeId().equals(
                        invoice.getEmployeeId()
                )
        )) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        }

        return new Receipt(
                invoice,
                invoices.payment(id).orElseThrow(
                        () -> new IllegalStateException(
                                "Payment missing"
                        )
                ),
                invoices.posLines(id),
                trades.linkedIds(id)
        );
    }

    private static void owned(
            HoaDon invoice,
            Long employeeId
    ) {
        if (!"QUAY".equals(invoice.getChannel())
                || !employeeId.equals(
                invoice.getEmployeeId()
        )) {
            throw new AccessDeniedException(
                    "Request owner mismatch"
            );
        }
    }

    private static void validPrice(SanPham product) {
        if (product.getGiaBan() == null
                || product.getGiaBan().signum() <= 0
                || product.getGiaBan().scale() > 0) {
            throw bad(
                    "Giá sản phẩm không hợp lệ."
            );
        }
    }

    private static void validTotal(BigDecimal total) {
        if (total.signum() <= 0
                || total.compareTo(MAX) > 0) {
            throw bad(
                    "Tổng tiền không hợp lệ."
            );
        }
    }

    private static void validTrade(
            PhieuThuCu trade,
            Long customerId
    ) {
        if (customerId == null
                || !customerId.equals(
                trade.getCustomerId()
        )
                || trade.getInvoiceId() != null) {
            throw bad(
                    "Phiếu thu phải thuộc khách đã chọn "
                            + "và chưa liên kết hóa đơn."
            );
        }
    }

    private static void validateSerial(
            SanPham product,
            Long id,
            int quantity,
            SerialMay serial,
            Long warehouseId
    ) {
        boolean console = "Máy console".equals(
                product.getLoaiSanPham().getName()
        );

        if (console) {
            if (quantity != 1
                    || id == null
                    || serial == null
                    || !product.getMaSP().equals(
                    serial.getProductId()
            )
                    || !warehouseId.equals(
                    serial.getWarehouseId()
            )
                    || !"TRONG_KHO".equals(
                    serial.getStatus()
            )
                    || !product.getTinhTrang().equals(
                    serial.getCondition()
            )) {
                throw bad(
                        "Máy " + product.getTenSP()
                                + " cần một serial hợp lệ "
                                + "tại kho đã chọn, số lượng 1."
                );
            }
        } else if (id != null) {
            throw bad(
                    "Chỉ chọn serial cho máy console."
            );
        }
    }

    private static void checkStock(
            Quote quote,
            List<TonKhoRepository.CheckoutStock> rows
    ) {
        Map<Long, Long> available = new HashMap<>();
        Map<Long, Long> requested = new TreeMap<>();

        rows.forEach(row -> available.put(
                row.getProductId(),
                row.getQuantity().longValue()
        ));

        quote.lines().forEach(line -> requested.merge(
                line.productId(),
                (long) line.quantity(),
                Long::sum
        ));

        List<String> missing = new ArrayList<>();

        requested.forEach((id, quantity) -> {
            long have = available.getOrDefault(
                    id,
                    0L
            );

            if (quantity > have) {
                String name = quote.lines().stream()
                        .filter(
                                line -> line.productId().equals(id)
                        )
                        .findFirst()
                        .orElseThrow()
                        .name();

                missing.add(
                        name + ": cần " + quantity
                                + ", còn " + have
                );
            }
        });

        if (!missing.isEmpty()) {
            throw bad(
                    "Thiếu tồn tại kho: "
                            + String.join("; ", missing)
            );
        }
    }

    private static boolean deadlock(Throwable exception) {
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

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static PosRejectedException bad(String message) {
        return new PosRejectedException(message);
    }
}