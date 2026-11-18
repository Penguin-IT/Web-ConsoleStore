package com.example.consolestore.Service;

import com.example.consolestore.DTO.InvoiceData;
import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true, timeout = 20)
public class InvoiceService {

    private final HoaDonRepository invoices;
    private final TaiKhoanRepository accounts;

    public InvoiceService(
            HoaDonRepository invoices,
            TaiKhoanRepository accounts
    ) {
        this.invoices = invoices;
        this.accounts = accounts;
    }

    public Page<InvoiceData.Header> list(
            String username,
            String keyword,
            String channel,
            String status,
            String from,
            String to,
            String sort,
            int page
    ) {
        requireStaff(username);

        String query = clean(keyword);
        String selectedChannel = clean(channel);
        String selectedStatus = clean(status);
        String selectedSort = clean(sort);

        if (query.length() > 100) {
            throw invalid("Từ khóa tối đa 100 ký tự.");
        }

        if (!Set.of("", "QUAY", "ONLINE")
                .contains(selectedChannel)) {
            throw invalid("Kênh bán không hợp lệ.");
        }

        if (!Set.of(
                "",
                "CHO_XAC_NHAN",
                "DA_XAC_NHAN",
                "DANG_GIAO",
                "HOAN_TAT",
                "HUY"
        ).contains(selectedStatus)) {
            throw invalid("Trạng thái hóa đơn không hợp lệ.");
        }

        if (!Set.of(
                "newest",
                "oldest",
                "amount-asc",
                "amount-desc"
        ).contains(selectedSort)) {
            throw invalid("Cách sắp xếp không hợp lệ.");
        }

        if (page < 0 || page > 1_000_000) {
            throw invalid("Trang không hợp lệ.");
        }

        LocalDate start = parseDate(from);
        LocalDate end = parseDate(to);

        if (start != null
                && end != null
                && start.isAfter(end)) {
            throw invalid(
                    "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc."
            );
        }

        LocalDateTime fromDate = start == null
                ? null
                : start.atStartOfDay();

        LocalDateTime toExclusive = end == null
                ? null
                : end.plusDays(1).atStartOfDay();

        String pattern = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return invoices.searchInvoices(
                pattern,
                selectedChannel,
                selectedStatus,
                fromDate,
                toExclusive,
                selectedSort,
                PageRequest.of(page, 15)
        ).map(this::header);
    }

    @Transactional(
            readOnly = true,
            timeout = 20,
            isolation = Isolation.REPEATABLE_READ
    )
    public InvoiceData.Detail detail(
            String username,
            Long id
    ) {
        requireStaff(username);

        var invoice = invoices.staffInvoice(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hóa đơn."
                ));

        List<InvoiceData.Line> lines =
                invoices.staffInvoiceLines(id)
                        .stream()
                        .map(line -> new InvoiceData.Line(
                                line.getProductId(),
                                line.getProductName(),
                                "CU".equals(line.getProductCondition())
                                        ? "Cũ"
                                        : "Mới",
                                display(line.getSerialNumber()),
                                display(line.getWarehouseName()),
                                line.getQuantity(),
                                line.getPrice(),
                                line.getSubtotal()
                        ))
                        .toList();

        List<InvoiceData.Payment> payments =
                invoices.staffInvoicePayments(id)
                        .stream()
                        .map(payment -> new InvoiceData.Payment(
                                payment.getId(),
                                payment.getMethod(),
                                payment.getAmount(),
                                payment.getStatus(),
                                payment.getPaidAt()
                        ))
                        .toList();

        return new InvoiceData.Detail(
                header(invoice),
                lines,
                payments
        );
    }

    private InvoiceData.Header header(
            HoaDonRepository.InvoiceHeader source
    ) {
        return new InvoiceData.Header(
                source.getId(),
                source.getCreatedAt(),
                source.getCustomerName(),
                display(source.getPhone()),
                display(source.getAddress()),
                display(source.getEmployeeName()),
                display(source.getWarehouseName()),
                source.getChannel(),
                source.getStatus(),
                source.getTotalAmount(),
                display(source.getNote())
        );
    }

    private LocalDate parseDate(String value) {
        if (clean(value).isBlank()) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(value);

            if (date.getYear() < 1753 || date.getYear() > 9998) {
                throw invalid("Ngày nằm ngoài phạm vi hỗ trợ.");
            }

            return date;
        } catch (DateTimeParseException exception) {
            throw invalid("Ngày lọc hóa đơn không hợp lệ.");
        }
    }


    private void requireStaff(String username) {
        if (username == null || username.isBlank()) {
            throw denied();
        }

        var state = accounts.loginState(username)
                .orElseThrow(this::denied);

        if (!state.isUsable()
                || !Set.of(
                "ADMIN",
                "NHANVIEN",
                "BANHANG"
        ).contains(state.getRole())) {
            throw denied();
        }
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException(
                "Bạn không có quyền xem hóa đơn."
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private String display(String value) {
        return clean(value).isBlank() ? "—" : value;
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}