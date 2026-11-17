package com.example.consolestore.Service;

import com.example.consolestore.DTO.PurchaseCostReport;
import com.example.consolestore.DTO.StockReport;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true, timeout = 20)
public class StoreReportService {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MM/yyyy");

    private final PhieuNhapRepository purchases;
    private final HoaDonRepository invoices;
    private final TonKhoRepository stocks;
    private final NhaCungCapRepository suppliers;
    private final TaiKhoanRepository accounts;

    public StoreReportService(
            PhieuNhapRepository purchases,
            HoaDonRepository invoices,
            TonKhoRepository stocks,
            NhaCungCapRepository suppliers,
            TaiKhoanRepository accounts
    ) {
        this.purchases = purchases;
        this.invoices = invoices;
        this.stocks = stocks;
        this.suppliers = suppliers;
        this.accounts = accounts;
    }

    public record SupplierOption(Long id, String name) {
    }

    public List<SupplierOption> supplierOptions(String username) {
        admin(username);

        return suppliers.findAll(Sort.by("name"))
                .stream()
                .map(supplier -> new SupplierOption(
                        supplier.getId(),
                        supplier.getName()
                ))
                .toList();
    }

    public PurchaseCostReport purchaseCosts(
            String username,
            String fromMonth,
            String toMonth,
            String supplierValue
    ) {
        admin(username);

        YearMonth start;
        YearMonth end;

        try {
            start = YearMonth.parse(fromMonth);
            end = YearMonth.parse(toMonth);
        } catch (DateTimeParseException exception) {
            throw invalid("Tháng báo cáo không hợp lệ.");
        }

        if (start.getYear() < 1753
                || end.getYear() > 9998
                || start.isAfter(end)) {
            throw invalid("Khoảng tháng không hợp lệ.");
        }

        if (ChronoUnit.MONTHS.between(start, end) >= 120) {
            throw invalid("Mỗi báo cáo tối đa 120 tháng.");
        }

        Long supplierId = optionalId(supplierValue);

        if (supplierId != null
                && !suppliers.existsById(supplierId)) {
            throw invalid("Nhà cung cấp không tồn tại.");
        }

        Map<YearMonth, PurchaseCostReport.MonthRow> monthRows =
                new LinkedHashMap<>();

        Map<Long, PurchaseCostReport.SupplierRow> supplierRows =
                new HashMap<>();

        for (YearMonth month = start;
             !month.isAfter(end);
             month = month.plusMonths(1)) {

            monthRows.put(
                    month,
                    new PurchaseCostReport.MonthRow(
                            month.format(MONTH_FORMAT),
                            0,
                            BigDecimal.ZERO
                    )
            );
        }

        List<PhieuNhapRepository.PurchaseCostSummary> data =
                purchases.purchaseCostSummary(
                        start.atDay(1).atStartOfDay(),
                        end.plusMonths(1).atDay(1).atStartOfDay(),
                        supplierId
                );

        for (PhieuNhapRepository.PurchaseCostSummary item : data) {
            YearMonth month = YearMonth.parse(
                    item.getReportMonth()
            );

            PurchaseCostReport.MonthRow current =
                    monthRows.get(month);

            BigDecimal amount = item.getAmount();
            long receiptCount = item.getReceiptCount();

            monthRows.put(
                    month,
                    new PurchaseCostReport.MonthRow(
                            current.label(),
                            current.receiptCount() + receiptCount,
                            current.amount().add(amount)
                    )
            );

            supplierRows.compute(
                    item.getSupplierId(),
                    (id, previous) -> new PurchaseCostReport.SupplierRow(
                            id,
                            item.getSupplierName(),
                            receiptCount + (
                                    previous == null
                                            ? 0
                                            : previous.receiptCount()
                            ),
                            amount.add(
                                    previous == null
                                            ? BigDecimal.ZERO
                                            : previous.amount()
                            )
                    )
            );
        }

        List<PurchaseCostReport.SupplierRow> sortedSuppliers =
                supplierRows.values()
                        .stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                PurchaseCostReport
                                                        .SupplierRow::amount
                                        )
                                        .reversed()
                                        .thenComparing(
                                                PurchaseCostReport
                                                        .SupplierRow::id
                                        )
                        )
                        .toList();

        long receiptCount = monthRows.values()
                .stream()
                .mapToLong(PurchaseCostReport.MonthRow::receiptCount)
                .sum();

        BigDecimal total = monthRows.values()
                .stream()
                .map(PurchaseCostReport.MonthRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseCostReport(
                start.format(MONTH_FORMAT)
                        + " – "
                        + end.format(MONTH_FORMAT),
                List.copyOf(monthRows.values()),
                sortedSuppliers,
                receiptCount,
                total
        );
    }

    public StockReport stockReport(
            String username,
            String fromValue,
            String toValue,
            String topValue,
            String thresholdValue,
            String pageValue
    ) {
        admin(username);

        LocalDate start;
        LocalDate end;

        try {
            start = LocalDate.parse(fromValue);
            end = LocalDate.parse(toValue);
        } catch (DateTimeParseException exception) {
            throw invalid("Ngày báo cáo không hợp lệ.");
        }

        if (start.isBefore(LocalDate.of(1753, 1, 1))
                || end.isAfter(LocalDate.of(9998, 12, 31))
                || start.isAfter(end)) {
            throw invalid("Khoảng ngày không hợp lệ.");
        }

        if (ChronoUnit.DAYS.between(start, end) >= 3660) {
            throw invalid("Khoảng thống kê tối đa 3.660 ngày.");
        }

        int top = number(topValue, 1, 50, "Top sản phẩm");

        int threshold = number(
                thresholdValue,
                1,
                1_000_000,
                "Ngưỡng tồn"
        );

        int page = number(
                pageValue,
                0,
                1_000_000,
                "Trang"
        );

        List<StockReport.BestSeller> bestSellers =
                invoices.bestSellers(
                        start.atStartOfDay(),
                        end.plusDays(1).atStartOfDay(),
                        top
                ).stream().map(item -> new StockReport.BestSeller(
                        item.getProductId(),
                        item.getProductName(),
                        CatalogService.imagePath(item.getImage()),
                        condition(item.getProductCondition()),
                        Boolean.TRUE.equals(item.getDeleted()),
                        item.getSoldQuantity(),
                        item.getRevenue()
                )).toList();

        Page<TonKhoRepository.LowStockSummary> lowStock =
                stocks.lowStock(
                        threshold,
                        PageRequest.of(page, 15)
                );

        if (lowStock.getTotalPages() > 0
                && page >= lowStock.getTotalPages()) {
            lowStock = stocks.lowStock(
                    threshold,
                    PageRequest.of(
                            lowStock.getTotalPages() - 1,
                            15
                    )
            );
        }

        Page<StockReport.LowStock> lowStockRows =
                lowStock.map(item -> new StockReport.LowStock(
                        item.getProductId(),
                        item.getProductName(),
                        CatalogService.imagePath(item.getImage()),
                        item.getCategory(),
                        condition(item.getProductCondition()),
                        item.getQuantity()
                ));

        return new StockReport(
                start.format(DAY_FORMAT)
                        + " – "
                        + end.format(DAY_FORMAT),
                bestSellers,
                lowStockRows,
                threshold
        );
    }

    private Long optionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw invalid("Mã nhà cung cấp không hợp lệ.");
            }

            return id;
        } catch (NumberFormatException exception) {
            throw invalid("Mã nhà cung cấp không hợp lệ.");
        }
    }

    private int number(
            String value,
            int min,
            int max,
            String label
    ) {
        try {
            int result = Integer.parseInt(value);

            if (result < min || result > max) {
                throw invalid(
                        label + " phải từ " + min + " đến " + max + "."
                );
            }

            return result;
        } catch (NumberFormatException exception) {
            throw invalid(label + " không hợp lệ.");
        }
    }

    private String condition(String value) {
        return "CU".equals(value) ? "Cũ" : "Mới";
    }

    private void admin(String username) {
        if (username == null
                || accounts.paymentAdmin(username).isEmpty()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xem báo cáo."
            );
        }
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}