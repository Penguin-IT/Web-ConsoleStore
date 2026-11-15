package com.example.consolestore.Service;

import com.example.consolestore.Dto.*;
import com.example.consolestore.Entity.*;
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
public class PurchaseService {

    private static final BigDecimal MAX_MONEY =
            new BigDecimal("999999999999999999");

    private final PhieuNhapRepository receipts;
    private final NhaCungCapRepository suppliers;
    private final KhoRepository warehouses;
    private final SanPhamRepository products;
    private final TonKhoRepository stocks;
    private final SerialMayRepository serials;
    private final TaiKhoanRepository accounts;

    public PurchaseService(
            PhieuNhapRepository receipts,
            NhaCungCapRepository suppliers,
            KhoRepository warehouses,
            SanPhamRepository products,
            TonKhoRepository stocks,
            SerialMayRepository serials,
            TaiKhoanRepository accounts
    ) {
        this.receipts = receipts;
        this.suppliers = suppliers;
        this.warehouses = warehouses;
        this.products = products;
        this.stocks = stocks;
        this.serials = serials;
        this.accounts = accounts;
    }

    public PurchaseData.Options options(String username) {
        admin(username);

        return new PurchaseData.Options(
                suppliers.findAll(Sort.by("id")),
                warehouses.findAll(Sort.by("id")),
                receipts.productOptions()
        );
    }

    public Page<PhieuNhapRepository.ReceiptRow> list(
            String username,
            String keyword,
            Long supplierId,
            Long warehouseId,
            int page
    ) {
        admin(username);

        if (supplierId != null
                && (supplierId < 1
                || !suppliers.existsById(supplierId))) {
            throw bad("Nhà cung cấp không tồn tại.");
        }

        if (warehouseId != null
                && (warehouseId < 1
                || !warehouses.existsById(warehouseId))) {
            throw bad("Kho không tồn tại.");
        }

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

        return receipts.search(
                supplierId,
                warehouseId,
                query,
                PageRequest.of(Math.max(0, page), 20)
        );
    }

    public PurchaseData.Detail detail(
            String username,
            Long id
    ) {
        admin(username);

        var header = receipts.header(id)
                .orElseThrow(PurchaseService::notFound);

        var lines = receipts.lines(id);

        Map<Long, List<String>> numbers = new LinkedHashMap<>();

        lines.forEach(line ->
                numbers.put(
                        line.getId(),
                        new ArrayList<>()
                )
        );

        receipts.serialRows(id).forEach(row ->
                numbers.computeIfAbsent(
                        row.getLineId(),
                        key -> new ArrayList<>()
                ).add(row.getNumber())
        );

        return new PurchaseData.Detail(
                header,
                lines,
                numbers,
                visibleNote(header.getNote())
        );
    }

    public Optional<Long> existing(
            String username,
            String token
    ) {
        var actor = admin(username);

        return receipts.findByTag(tag(token)).map(receipt -> {
            if (!Objects.equals(
                    actor.getEmployeeId(),
                    receipt.getEmployeeId()
            )) {
                throw new AccessDeniedException(
                        "Receipt owner required"
                );
            }

            return receipt.getId();
        });
    }

    private record Checked(
            SanPham product,
            int quantity,
            BigDecimal price,
            List<String> numbers
    ) {
    }

    @Transactional(timeout = 30)
    public Long create(
            String username,
            PurchaseRequest form
    ) {
        var actor = admin(username);

        if (actor.getEmployeeId() == null) {
            throw bad(
                    "Tài khoản ADMIN cần liên kết nhân viên "
                            + "để lập phiếu nhập."
            );
        }

        String requestTag = tag(form.getToken());

        var previous = receipts.lockByTag(requestTag);

        if (previous.isPresent()) {
            if (!actor.getEmployeeId().equals(
                    previous.get().getEmployeeId()
            )) {
                throw new AccessDeniedException(
                        "Receipt owner required"
                );
            }

            return previous.get().getId();
        }

        if (form.getSupplierId() == null
                || !suppliers.existsById(
                form.getSupplierId()
        )) {
            throw bad("Chọn nhà cung cấp hợp lệ.");
        }

        if (form.getWarehouseId() == null
                || !warehouses.existsById(
                form.getWarehouseId()
        )) {
            throw bad("Chọn kho nhận hợp lệ.");
        }

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
            throw bad("Cần chọn từ 1 đến 30 dòng sản phẩm.");
        }

        var ids = items.stream()
                .map(PurchaseRequest.Item::getProductId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, SanPham> byId = new HashMap<>();

        products.lockForCheckout(ids).forEach(product ->
                byId.put(product.getMaSP(), product)
        );

        List<Checked> checked = new ArrayList<>();
        Set<String> allNumbers = new TreeSet<>();
        Map<Long, Long> quantities = new TreeMap<>();

        BigDecimal total = BigDecimal.ZERO;

        for (var item : items) {
            var product = byId.get(item.getProductId());

            if (product == null
                    || product.isDeleted()
                    || !Set.of("MOI", "CU").contains(
                    product.getTinhTrang()
            )
                    || (
                    "Đĩa game".equals(
                            product.getLoaiSanPham().getName()
                    )
                            && "CU".equals(product.getTinhTrang())
            )) {
                throw bad(
                        "Sản phẩm không hợp lệ để nhập kho."
                );
            }

            if (item.getQuantity() == null
                    || item.getQuantity() < 1) {
                throw bad("Số lượng phải lớn hơn 0.");
            }

            String priceText = clean(item.getPrice());

            if (!priceText.matches("[0-9]{1,18}")) {
                throw bad(
                        "Giá nhập phải là số nguyên không âm."
                );
            }

            BigDecimal price = new BigDecimal(priceText);

            String rawNumbers = clean(item.getSerials());

            if (rawNumbers.length() > 22000) {
                throw bad("Danh sách serial quá dài.");
            }

            List<String> numbers = rawNumbers.lines()
                    .map(String::strip)
                    .filter(number -> !number.isEmpty())
                    .map(number ->
                            number.toUpperCase(Locale.ROOT)
                    )
                    .toList();

            boolean machine = "Máy console".equals(
                    product.getLoaiSanPham().getName()
            );

            if (machine) {
                if (numbers.size() != item.getQuantity()) {
                    throw bad(
                            "Số serial phải bằng số lượng máy nhập."
                    );
                }

                for (String number : numbers) {
                    if (!number.matches(
                            "[A-Z0-9][A-Z0-9._/-]{0,99}"
                    ) || !allNumbers.add(number)) {
                        throw bad(
                                "Serial không hợp lệ "
                                        + "hoặc bị trùng trong phiếu."
                        );
                    }
                }

                if (allNumbers.size() > 200) {
                    throw bad(
                            "Mỗi phiếu tối đa 200 serial máy."
                    );
                }
            } else if (!numbers.isEmpty()) {
                throw bad(
                        "Chỉ nhập serial cho máy console."
                );
            }

            quantities.merge(
                    product.getMaSP(),
                    item.getQuantity().longValue(),
                    Long::sum
            );

            total = total.add(
                    price.multiply(
                            BigDecimal.valueOf(
                                    item.getQuantity()
                            )
                    )
            );

            if (total.compareTo(MAX_MONEY) > 0) {
                throw bad("Tổng tiền vượt giới hạn.");
            }

            checked.add(new Checked(
                    product,
                    item.getQuantity(),
                    price,
                    numbers
            ));
        }

        Map<Long, Long> available = new HashMap<>();

        stocks.lockPosStocks(
                form.getWarehouseId(),
                ids
        ).forEach(row ->
                available.put(
                        row.getProductId(),
                        row.getQuantity().longValue()
                )
        );

        for (var entry : quantities.entrySet()) {
            long after = available.getOrDefault(
                    entry.getKey(),
                    0L
            ) + entry.getValue();

            if (after > Integer.MAX_VALUE) {
                throw bad(
                        "Số lượng tồn kho vượt giới hạn."
                );
            }
        }

        for (String number : allNumbers) {
            if (serials.lockByNumber(number).isPresent()) {
                throw bad(
                        "Serial đã tồn tại: " + number
                );
            }
        }

        var receipt = receipts.saveAndFlush(
                new PhieuNhap(
                        form.getSupplierId(),
                        actor.getEmployeeId(),
                        form.getWarehouseId(),
                        requestTag + "|" + note
                )
        );

        checked.sort(
                Comparator.comparing(
                        row -> row.product().getMaSP()
                )
        );

        for (var row : checked) {
            receipts.insertLine(
                    receipt.getId(),
                    row.product().getMaSP(),
                    row.quantity(),
                    row.price()
            );

            Long lineId = receipts.lastLineId(
                    receipt.getId(),
                    row.product().getMaSP()
            ).orElseThrow(() -> bad(
                    "Chưa ghi nhận được dòng nhập."
            ));

            for (String number : row.numbers()) {
                serials.insertPurchasedSerial(
                        row.product().getMaSP(),
                        form.getWarehouseId(),
                        number,
                        row.product().getTinhTrang()
                );

                var saved = serials.lockByNumber(number)
                        .orElseThrow(() -> bad(
                                "Chưa tạo được serial."
                        ));

                receipts.linkSerial(
                        lineId,
                        saved.getId()
                );
            }

            if (receipts.serialCount(lineId)
                    != row.numbers().size()) {
                throw bad(
                        "Số serial liên kết không khớp."
                );
            }
        }

        if (total.compareTo(
                receipts.readTotal(receipt.getId())
        ) != 0) {
            throw bad(
                    "Tổng tiền phiếu nhập không khớp."
            );
        }

        return receipt.getId();
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

    private static String tag(String value) {
        try {
            if (value == null
                    || !UUID.fromString(value)
                    .toString()
                    .equals(value)) {
                throw new IllegalArgumentException();
            }

            return "CN16:" + value;
        } catch (IllegalArgumentException exception) {
            throw bad(
                    "Phiên lập phiếu không hợp lệ."
            );
        }
    }

    private static String visibleNote(String value) {
        String note = clean(value);

        if (note.length() >= 42
                && note.substring(0, 42).matches(
                "^CN16:[0-9a-f-]{36}\\|$"
        )) {
            return note.substring(42);
        }

        return note;
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