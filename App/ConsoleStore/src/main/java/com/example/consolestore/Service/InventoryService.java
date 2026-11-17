package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.Kho;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final TonKhoRepository stocks;
    private final BienDongKhoRepository movements;
    private final KhoRepository warehouses;
    private final SanPhamRepository products;
    private final TaiKhoanRepository accounts;

    public InventoryService(
            TonKhoRepository stocks,
            BienDongKhoRepository movements,
            KhoRepository warehouses,
            SanPhamRepository products,
            TaiKhoanRepository accounts
    ) {
        this.stocks = stocks;
        this.movements = movements;
        this.warehouses = warehouses;
        this.products = products;
        this.accounts = accounts;
    }

    public List<Kho> warehouses(String username) {
        staff(username);

        return warehouses.findAll(Sort.by("id"));
    }

    public Page<TonKhoRepository.InventoryRow> list(
            String username,
            InventoryFilter filter,
            int page
    ) {
        staff(username);
        warehouse(filter.getWarehouseId());

        String condition = clean(filter.getCondition());
        String availability = clean(filter.getAvailability());

        if (!Set.of("", "MOI", "CU").contains(condition)) {
            throw bad("Tình trạng không hợp lệ.");
        }

        if (!Set.of(
                "ALL",
                "IN_STOCK",
                "OUT_OF_STOCK"
        ).contains(availability)) {
            throw bad("Bộ lọc tồn kho không hợp lệ.");
        }

        String keyword = clean(filter.getKeyword());

        if (keyword.length() > 100) {
            throw bad("Từ khóa tối đa 100 ký tự.");
        }

        keyword = "%"
                + keyword.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return stocks.inventorySearch(
                filter.getWarehouseId(),
                condition,
                availability,
                keyword,
                PageRequest.of(Math.max(0, page), 20)
        );
    }

    public InventoryHistory history(
            String username,
            Long productId,
            InventoryFilter filter,
            int page
    ) {
        staff(username);
        warehouse(filter.getWarehouseId());

        var product = products.findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sản phẩm."
                        )
                );

        String type = clean(filter.getType());

        if (!Set.of(
                "",
                "NHAP",
                "BAN",
                "THU_CU",
                "HOAN_HUY"
        ).contains(type)) {
            throw bad("Loại biến động không hợp lệ.");
        }

        LocalDate from = filter.getFrom();
        LocalDate to = filter.getTo();

        validDate(from);
        validDate(to);

        if (from != null && to != null && from.isAfter(to)) {
            throw bad(
                    "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc."
            );
        }

        LocalDateTime fromAt = from == null
                ? LocalDateTime.of(1753, 1, 1, 0, 0)
                : from.atStartOfDay();

        LocalDateTime toAt = to == null
                ? LocalDateTime.of(9999, 1, 1, 0, 0)
                : to.plusDays(1).atStartOfDay();

        return new InventoryHistory(
                product.getMaSP(),
                product.getTenSP(),
                product.getTinhTrang(),
                product.isDeleted(),
                stocks.inventoryQuantity(
                        productId,
                        filter.getWarehouseId()
                ),
                movements.search(
                        productId,
                        filter.getWarehouseId(),
                        type,
                        fromAt,
                        toAt,
                        PageRequest.of(Math.max(0, page), 20)
                )
        );
    }

    private void warehouse(Long id) {
        if (id != null
                && (id < 1 || !warehouses.existsById(id))) {
            throw bad("Kho không tồn tại.");
        }
    }


    private void staff(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException(
                    "Bạn chưa đăng nhập."
            );
        }

        TaiKhoanRepository.LoginState state =
                accounts.loginState(username)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Không tìm thấy tài khoản."
                                )
                        );

        if (!state.isUsable()
                || !Set.of(
                "ADMIN",
                "NHANVIEN",
                "KHO"
        ).contains(state.getRole())) {
            throw new AccessDeniedException(
                    "Bạn không có quyền truy cập quản lý kho."
            );
        }
    }


    private static void validDate(LocalDate date) {
        if (date != null
                && (date.getYear() < 1753
                || date.getYear() > 9998)) {
            throw bad("Ngày lọc nằm ngoài phạm vi hỗ trợ.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}