package com.example.consolestore.Service;

import com.example.consolestore.Repository.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true, timeout = 10)
public class LookupService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TaiKhoanRepository accounts;
    private final TonKhoRepository stocks;
    private final PhieuBaoHanhRepository warranties;
    private final PasswordEncoder passwordEncoder;

    public LookupService(
            TaiKhoanRepository accounts,
            TonKhoRepository stocks,
            PhieuBaoHanhRepository warranties,
            PasswordEncoder passwordEncoder
    ) {
        this.accounts = accounts;
        this.stocks = stocks;
        this.warranties = warranties;
        this.passwordEncoder = passwordEncoder;
    }

    public record StaffSession(
            Long accountId,
            String username
    ) {
    }

    public StaffSession authenticate(
            String username,
            String password
    ) {
        String login = clean(username)
                .toLowerCase(Locale.ROOT);

        if (login.isBlank()
                || login.length() > 100
                || password == null
                || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw denied();
        }

        var account = accounts.findByUsernameIgnoreCase(login)
                .orElseThrow(this::denied);

        var state = accounts.loginState(login)
                .orElseThrow(this::denied);

        if (!state.isUsable()
                || !Set.of("ADMIN", "NHANVIEN")
                .contains(state.getRole())) {
            throw denied();
        }

        boolean matches;

        try {
            matches = passwordEncoder.matches(
                    password,
                    account.getPasswordHash()
            );
        } catch (IllegalArgumentException exception) {
            throw denied();
        }

        if (!matches) {
            throw denied();
        }

        return new StaffSession(
                account.getId(),
                account.getUsername()
        );
    }

    public String stock(
            StaffSession session,
            String value
    ) {
        requireStaff(session);

        String query = clean(value);

        if (query.isBlank() || query.length() > 100) {
            throw new IllegalArgumentException(
                    "Nhập mã hoặc tên sản phẩm, tối đa 100 ký tự."
            );
        }

        Long productId = null;

        if (query.matches("(?i)(SP)?[1-9][0-9]*")) {
            String number = query.replaceFirst("(?i)^SP", "");

            try {
                productId = Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Mã sản phẩm không hợp lệ."
                );
            }
        }

        String keyword = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        var rows = stocks.socketStock(productId, keyword);

        if (rows.isEmpty()) {
            return "Không tìm thấy sản phẩm đang bán.";
        }

        StringBuilder result = new StringBuilder(
                "TỒN KHẢ DỤNG — TẤT CẢ KHO\n"
        );

        for (var row : rows) {
            result.append("\nSP")
                    .append(row.getProductId())
                    .append(" | ")
                    .append(row.getProductName())
                    .append(" | ")
                    .append(
                            "CU".equals(row.getProductCondition())
                                    ? "Cũ"
                                    : "Mới"
                    )
                    .append(" | Tồn: ")
                    .append(row.getQuantity());
        }

        result.append("\n\nHiển thị tối đa 20 sản phẩm.");

        return result.toString();
    }

    public String warranty(
            StaffSession session,
            String value
    ) {
        requireStaff(session);

        String serial = clean(value);

        if (serial.isBlank() || serial.length() > 100) {
            throw new IllegalArgumentException(
                    "Serial bắt buộc, tối đa 100 ký tự."
            );
        }

        var item = warranties.lookup(serial).orElse(null);

        if (item == null) {
            return "Không tìm thấy serial trong hệ thống.";
        }

        StringBuilder result = new StringBuilder();

        result.append("TRA CỨU BẢO HÀNH")
                .append("\nSerial: ")
                .append(item.getNumber())
                .append("\nSản phẩm: ")
                .append(item.getProductName())
                .append("\nTrạng thái máy: ")
                .append(serialStatus(item.getSerialStatus()));

        if (item.getInvoiceId() == null) {
            result.append("\nChưa ghi nhận giao dịch bán cho serial này.");
        } else {
            result.append("\nHóa đơn: HD")
                    .append(item.getInvoiceId())
                    .append("\nNgày bán: ")
                    .append(date(item.getSoldAt()))
                    .append("\nHạn bảo hành: ")
                    .append(date(item.getExpiresAt()))
                    .append("\nBảo hành: ")
                    .append(
                            Boolean.TRUE.equals(item.getCovered())
                                    ? "Còn hạn"
                                    : "Không còn trong thời hạn"
                    );
        }

        if (item.getActiveId() != null) {
            result.append("\nPhiếu đang xử lý: BH")
                    .append(item.getActiveId());
        }

        return result.toString();
    }

    private void requireStaff(StaffSession session) {
        if (session == null) {
            throw denied();
        }

        var state = accounts.loginState(session.username())
                .orElseThrow(this::denied);

        if (!Objects.equals(
                state.getAccountId(),
                session.accountId()
        ) || !state.isUsable()
                || !Set.of("ADMIN", "NHANVIEN")
                .contains(state.getRole())) {
            throw denied();
        }
    }

    private String serialStatus(String value) {
        if (value == null) {
            return "Không xác định";
        }

        return switch (value) {
            case "TRONG_KHO" -> "Trong kho";
            case "DA_BAN" -> "Đã bán";
            case "BAO_HANH" -> "Đang bảo hành";
            default -> value;
        };
    }

    private String date(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_FORMAT);
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException(
                "Tài khoản không hợp lệ hoặc không có quyền tra cứu."
        );
    }
}