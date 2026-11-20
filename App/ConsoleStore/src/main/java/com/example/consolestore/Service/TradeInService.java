package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class TradeInService {

    private static final BigDecimal MAX_MONEY =
            new BigDecimal("999999999999999999");

    private final PhieuThuCuRepository receipts;
    private final TaiKhoanRepository accounts;
    private final KhachHangRepository customers;
    private final KhoRepository warehouses;
    private final SanPhamRepository products;
    private final TonKhoRepository stocks;
    private final SerialMayRepository serials;

    public TradeInService(
            PhieuThuCuRepository receipts,
            TaiKhoanRepository accounts,
            KhachHangRepository customers,
            KhoRepository warehouses,
            SanPhamRepository products,
            TonKhoRepository stocks,
            SerialMayRepository serials
    ) {
        this.receipts = receipts;
        this.accounts = accounts;
        this.customers = customers;
        this.warehouses = warehouses;
        this.products = products;
        this.stocks = stocks;
        this.serials = serials;
    }

    @Transactional(readOnly = true)
    public Page<PhieuThuCuRepository.ReceiptRow> list(
            String username,
            String keyword,
            String from,
            String to,
            int page
    ) {
        staff(username);

        String query = clean(keyword);

        if (query.length() > 100) {
            throw bad("Từ khóa tối đa 100 ký tự.");
        }

        query = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        LocalDate start = parseDate(from);
        LocalDate end = parseDate(to);

        if (start != null && end != null && start.isAfter(end)) {
            throw bad("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }

        LocalDateTime fromDate = start == null ? null : start.atStartOfDay();
        LocalDateTime toExclusive = end == null ? null : end.plusDays(1).atStartOfDay();

        return receipts.searchReceipts(
                query,
                fromDate,
                toExclusive,
                PageRequest.of(Math.max(0, page), 10)
        );
    }

    @Transactional(readOnly = true)
    public TradeInData.Options options(
            String username,
            String phone
    ) {
        staff(username);

        return new TradeInData.Options(
                warehouses.findAll(Sort.by("id")),
                receipts.usedProducts(),
                customers.findByPhone(clean(phone)).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public Optional<Long> existing(
            String username,
            String token
    ) {
        Long employeeId = staff(username);

        return receipts.findByTag(tag(token)).map(receipt -> {
            if (!employeeId.equals(receipt.getEmployeeId())) {
                throw new AccessDeniedException(
                        "Receipt owner required"
                );
            }

            return receipt.getId();
        });
    }

    @Transactional(readOnly = true)
    public TradeInData.Detail detail(
            String username,
            Long id
    ) {
        staff(username);

        var receipt = receipts.findById(id)
                .orElseThrow(TradeInService::notFound);

        String note = clean(receipt.getNote());

        boolean cashPaid = note.length() >= 52
                && note.substring(0, 52).matches(
                "^CN11:[0-9a-f-]{36}\\|CASH_PAID\\|$"
        );

        if (cashPaid) {
            note = note.substring(52);
        }

        return new TradeInData.Detail(
                receipt,
                customers.findById(receipt.getCustomerId())
                        .orElseThrow(TradeInService::notFound),
                warehouses.findById(receipt.getWarehouseId())
                        .orElseThrow(TradeInService::notFound),
                receipts.tradeLines(id),
                note,
                cashPaid
        );
    }

    private record Checked(
            SanPham product,
            int quantity,
            BigDecimal price,
            String serial,
            String condition,
            String note
    ) {
    }

    @Transactional(timeout = 30)
    public Long create(
            String username,
            TradeInRequest form
    ) {
        Long employeeId = staff(username);
        String requestTag = tag(form.getToken());

        var previous = receipts.lockByTag(requestTag);

        if (previous.isPresent()) {
            if (!employeeId.equals(
                    previous.get().getEmployeeId()
            )) {
                throw new AccessDeniedException(
                        "Receipt owner required"
                );
            }

            return previous.get().getId();
        }

        if (!form.isPaid()) {
            throw bad("Cần xác nhận đã chi đủ tiền mặt.");
        }

        String phone = clean(form.getPhone());

        if (!phone.matches("0[0-9]{9}")) {
            throw bad("Số điện thoại không hợp lệ.");
        }

        var customer = prepareCustomer(form, phone);

        if (form.getWarehouseId() == null) {
            throw bad("Vui lòng chọn kho nhận.");
        }

        var warehouse = warehouses.findById(
                form.getWarehouseId()
        ).orElseThrow(() -> bad(
                "Không tìm thấy kho nhận."
        ));

        String note = clean(form.getNote());

        if (note.length() > 180) {
            throw bad("Ghi chú tối đa 180 ký tự.");
        }

        var items = form.getItems();

        if (items == null
                || items.isEmpty()
                || items.size() > 30
                || items.stream().anyMatch(item ->
                item == null || item.getProductId() == null
        )) {
            throw bad("Cần chọn từ 1 đến 30 dòng thiết bị.");
        }

        var ids = items.stream()
                .map(TradeInRequest.Item::getProductId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, SanPham> byId = new HashMap<>();

        products.lockForCheckout(ids).forEach(product ->
                byId.put(product.getMaSP(), product)
        );

        List<Checked> checked = new ArrayList<>();
        Set<String> numbers = new TreeSet<>();
        Map<Long, Long> quantities = new TreeMap<>();

        BigDecimal total = BigDecimal.ZERO;

        for (var item : items) {
            SanPham product = byId.get(item.getProductId());

            if (product == null
                    || product.isDeleted()
                    || !"CU".equals(product.getTinhTrang())
                    || "Đĩa game".equals(
                    product.getLoaiSanPham().getName()
            )) {
                throw bad(
                        "Chỉ thu thiết bị có mã hàng cũ, "
                                + "không thu đĩa game."
                );
            }

            Integer quantity = item.getQuantity();

            if (quantity == null || quantity < 1) {
                throw bad("Số lượng phải lớn hơn 0.");
            }

            String priceText = clean(item.getPrice());

            if (!priceText.matches("[0-9]{1,18}")) {
                throw bad("Giá thu phải là số nguyên dương.");
            }

            BigDecimal price;

            try {
                price = new BigDecimal(priceText).setScale(0, java.math.RoundingMode.UNNECESSARY);
            } catch (NumberFormatException | ArithmeticException e) {    throw bad("Giá thu phải lớn hơn 0.");
            }

            String condition = clean(item.getCondition());
            String lineNote = clean(item.getNote());

            if (condition.isEmpty()
                    || condition.length() > 100
                    || lineNote.length() > 140) {
                throw bad(
                        "Nhập tình trạng tối đa 100 ký tự, "
                                + "ghi chú tối đa 140 ký tự."
                );
            }

            boolean machine = "Máy console".equals(
                    product.getLoaiSanPham().getName()
            );

            String number = clean(item.getSerial())
                    .toUpperCase(Locale.ROOT);

            if (machine) {
                if (quantity != 1
                        || !number.matches(
                        "[A-Z0-9][A-Z0-9._/-]{0,99}"
                )
                        || !numbers.add(number)) {
                    throw bad(
                            "Mỗi dòng máy cần số lượng 1 "
                                    + "và serial hợp lệ, không trùng."
                    );
                }
            } else if (!number.isEmpty()) {
                throw bad("Chỉ nhập serial cho máy console.");
            }

            quantities.merge(
                    product.getMaSP(),
                    quantity.longValue(),
                    Long::sum
            );

            total = total.add(
                    price.multiply(BigDecimal.valueOf(quantity))
            );

            if (total.compareTo(MAX_MONEY) > 0) {
                throw bad("Tổng tiền vượt giới hạn.");
            }

            checked.add(new Checked(
                    product,
                    quantity,
                    price,
                    number,
                    condition,
                    lineNote
            ));
        }

        Map<Long, Long> available = new HashMap<>();

        stocks.lockPosStocks(
                warehouse.getId(),
                ids
        ).forEach(stock ->
                available.put(
                        stock.getProductId(),
                        stock.getQuantity().longValue()
                )
        );

        for (var entry : quantities.entrySet()) {
            long after = available.getOrDefault(
                    entry.getKey(),
                    0L
            ) + entry.getValue();

            if (after > Integer.MAX_VALUE) {
                throw bad("Số lượng tồn kho vượt giới hạn.");
            }
        }

        Map<String, SerialMay> existingSerials = new HashMap<>();

        for (String number : numbers) {
            var found = serials.lockByNumber(number);

            if (found.isEmpty()) {
                continue;
            }

            var serial = found.get();

            var product = checked.stream()
                    .filter(row -> number.equals(row.serial()))
                    .findFirst()
                    .orElseThrow()
                    .product();

            boolean sameFamily =
                    product.getMaSP().equals(
                            serial.getProductId()
                    )
                            || Objects.equals(
                            product.getMaSPGoc(),
                            serial.getProductId()
                    );

            if (!sameFamily
                    || !"DA_BAN".equals(serial.getStatus())
                    || serials.activeWarranties(
                    serial.getId()
            ) > 0) {
                throw bad(
                        "Serial " + number
                                + " đã trong kho, đang bảo hành "
                                + "hoặc khác thiết bị."
                );
            }

            existingSerials.put(number, serial);
        }
        if (customer.getId() == null) {
            customer = customers.saveAndFlush(customer);
        }
        var receipt = receipts.saveAndFlush(
                new PhieuThuCu(
                        customer.getId(),
                        employeeId,
                        warehouse.getId(),
                        requestTag + "|CASH_PAID|" + note
                )
        );

        checked.sort(
                Comparator.comparing(
                        row -> row.product().getMaSP()
                )
        );

        for (Checked row : checked) {
            String detailNote = row.serial().isEmpty()
                    ? row.note()
                    : "Serial: " + row.serial()
                      + "; " + row.note();

            receipts.insertTradeLine(
                    receipt.getId(),
                    row.product().getMaSP(),
                    row.quantity(),
                    row.price(),
                    row.condition(),
                    detailNote
            );

            if (!row.serial().isEmpty()) {
                var old = existingSerials.get(row.serial());

                if (old == null) {
                    serials.insertReceivedSerial(
                            row.product().getMaSP(),
                            warehouse.getId(),
                            row.serial()
                    );
                } else if (
                        serials.receiveSoldSerial(
                                old.getId(),
                                row.product().getMaSP(),
                                warehouse.getId()
                        ) != 1
                ) {
                    throw bad(
                            "Serial vừa thay đổi. "
                                    + "Vui lòng kiểm tra lại."
                    );
                }
            }
        }

        if (total.compareTo(
                receipts.tradeTotal(receipt.getId())
        ) != 0) {
            throw bad("Tổng tiền phiếu thu không khớp.");
        }

        return receipt.getId();
    }

    private Long staff(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập.");
        }

        TaiKhoanRepository.LoginState state = accounts.loginState(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "Không tìm thấy tài khoản."
                ));

        if (!state.isUsable()
                || !Set.of("ADMIN", "NHANVIEN", "BAOHANH")
                .contains(state.getRole())
                || state.getEmployeeId() == null) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thực hiện thu mua cũ."
            );
        }

        return state.getEmployeeId();
    }

    private static String tag(String token) {
        try {
            if (token == null
                    || !UUID.fromString(token)
                    .toString()
                    .equals(token)) {
                throw new IllegalArgumentException();
            }

            return "CN11:" + token;
        } catch (IllegalArgumentException exception) {
            throw bad(
                    "Phiên lập phiếu không hợp lệ. Vui lòng tải lại."
            );
        }
    }

    private LocalDate parseDate(String value) {
        if (clean(value).isBlank()) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(value.strip());
            if (date.getYear() < 1753 || date.getYear() > 9998) {
                throw bad("Ngày nằm ngoài phạm vi hỗ trợ.");
            }
            return date;
        } catch (DateTimeParseException exception) {
            throw bad("Ngày tìm kiếm không hợp lệ.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private KhachHang prepareCustomer(
            TradeInRequest form,
            String phone
    ) {
        var existing = customers.lockByPhone(phone);

        if (existing.isPresent()) {
            if (form.isNewCustomer()) {
                throw bad(
                        "Số điện thoại đã có hồ sơ. "
                                + "Hãy tìm khách hàng và dùng hồ sơ hiện có."
                );
            }

            return existing.get();
        }

        if (!form.isNewCustomer()) {
            throw bad(
                    "Chưa có hồ sơ. "
                            + "Chọn thêm khách hàng mới và nhập thông tin."
            );
        }

        String fullName = clean(form.getFullName());
        String address = clean(form.getAddress());

        if (fullName.isEmpty() || fullName.length() > 100) {
            throw bad(
                    "Họ tên bắt buộc và tối đa 100 ký tự."
            );
        }

        if (address.length() > 255) {
            throw bad("Địa chỉ tối đa 255 ký tự.");
        }

        KhachHang customer = new KhachHang(
                fullName,
                phone,
                null
        );

        customer.updateProfile(
                fullName,
                phone,
                address
        );

        return customer;
    }
}
