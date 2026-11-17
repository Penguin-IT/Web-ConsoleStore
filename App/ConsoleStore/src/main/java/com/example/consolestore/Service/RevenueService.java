package com.example.consolestore.Service;

import com.example.consolestore.DTO.RevenueReport;
import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true, timeout = 20)
public class RevenueService {

    private static final LocalDate MIN_DATE =
            LocalDate.of(1753, 1, 1);

    private static final LocalDate MAX_DATE =
            LocalDate.of(9998, 12, 31);

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MM/yyyy");

    private final HoaDonRepository invoices;
    private final TaiKhoanRepository accounts;

    public RevenueService(
            HoaDonRepository invoices,
            TaiKhoanRepository accounts
    ) {
        this.invoices = invoices;
        this.accounts = accounts;
    }

    public RevenueReport report(
            String username,
            String mode,
            String month,
            String from,
            String to,
            String grouping
    ) {
        requireAdmin(username);

        LocalDate start;
        LocalDate end;
        String actualGrouping;

        try {
            if ("month".equals(mode)) {
                YearMonth selectedMonth = YearMonth.parse(month);

                start = selectedMonth.atDay(1);
                end = selectedMonth.atEndOfMonth();
                actualGrouping = "day";
            } else if ("range".equals(mode)) {
                start = LocalDate.parse(from);
                end = LocalDate.parse(to);

                if (!Set.of("day", "month").contains(grouping)) {
                    throw invalid("Cách tổng hợp không hợp lệ.");
                }

                actualGrouping = grouping;
            } else {
                throw invalid("Chế độ báo cáo không hợp lệ.");
            }
        } catch (DateTimeParseException exception) {
            throw invalid("Tháng hoặc khoảng ngày không hợp lệ.");
        }

        if (start.isBefore(MIN_DATE)
                || end.isAfter(MAX_DATE)) {
            throw invalid("Khoảng ngày nằm ngoài phạm vi hỗ trợ.");
        }

        if (start.isAfter(end)) {
            throw invalid("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        LocalDate firstBucket = bucket(start, actualGrouping);
        LocalDate lastBucket = bucket(end, actualGrouping);

        long bucketCount = "month".equals(actualGrouping)
                ? ChronoUnit.MONTHS.between(
                firstBucket,
                lastBucket
        ) + 1
                : ChronoUnit.DAYS.between(
                firstBucket,
                lastBucket
        ) + 1;

        long maximumBuckets = "month".equals(actualGrouping)
                ? 120
                : 366;

        if (bucketCount > maximumBuckets) {
            throw invalid(
                    "month".equals(actualGrouping)
                            ? "Mỗi báo cáo tổng hợp tối đa 120 tháng."
                            : "Mỗi báo cáo theo ngày tối đa 366 ngày. "
                            + "Hãy chọn tổng hợp theo tháng "
                            + "cho khoảng dài hơn."
            );
        }

        Map<LocalDate, Totals> buckets = new LinkedHashMap<>();

        for (LocalDate date = firstBucket;
             !date.isAfter(lastBucket);
             date = nextBucket(date, actualGrouping)) {

            buckets.put(date, new Totals());
        }

        List<HoaDonRepository.RevenueSummary> data =
                invoices.revenueSummary(
                        start.atStartOfDay(),
                        end.plusDays(1).atStartOfDay()
                );

        for (HoaDonRepository.RevenueSummary item : data) {
            LocalDate date = LocalDate.parse(item.getReportDay());

            Totals totals = buckets.get(
                    bucket(date, actualGrouping)
            );

            if (totals == null) {
                continue;
            }

            BigDecimal revenue = item.getRevenue() == null
                    ? BigDecimal.ZERO
                    : item.getRevenue();

            long count = item.getInvoiceCount() == null
                    ? 0
                    : item.getInvoiceCount();

            if ("QUAY".equals(item.getChannel())) {
                totals.counterRevenue =
                        totals.counterRevenue.add(revenue);

                totals.counterCount += count;
            } else if ("ONLINE".equals(item.getChannel())) {
                totals.onlineRevenue =
                        totals.onlineRevenue.add(revenue);

                totals.onlineCount += count;
            }
        }

        BigDecimal chartMaximum = BigDecimal.ZERO;
        BigDecimal counterRevenue = BigDecimal.ZERO;
        BigDecimal onlineRevenue = BigDecimal.ZERO;

        long counterCount = 0;
        long onlineCount = 0;

        for (Totals totals : buckets.values()) {
            chartMaximum = chartMaximum
                    .max(totals.counterRevenue)
                    .max(totals.onlineRevenue);

            counterRevenue = counterRevenue.add(
                    totals.counterRevenue
            );

            onlineRevenue = onlineRevenue.add(
                    totals.onlineRevenue
            );

            counterCount += totals.counterCount;
            onlineCount += totals.onlineCount;
        }

        List<RevenueReport.Row> rows = new ArrayList<>();

        DateTimeFormatter labelFormat =
                "month".equals(actualGrouping)
                        ? MONTH_FORMAT
                        : DAY_FORMAT;

        for (Map.Entry<LocalDate, Totals> entry
                : buckets.entrySet()) {

            Totals totals = entry.getValue();

            rows.add(new RevenueReport.Row(
                    entry.getKey().format(labelFormat),
                    totals.counterCount,
                    totals.onlineCount,
                    totals.counterRevenue,
                    totals.onlineRevenue,
                    percentage(
                            totals.counterRevenue,
                            chartMaximum
                    ),
                    percentage(
                            totals.onlineRevenue,
                            chartMaximum
                    )
            ));
        }

        return new RevenueReport(
                start.format(DAY_FORMAT)
                        + " – "
                        + end.format(DAY_FORMAT),
                actualGrouping,
                List.copyOf(rows),
                counterRevenue,
                onlineRevenue,
                counterCount,
                onlineCount,
                chartMaximum
        );
    }

    private LocalDate bucket(
            LocalDate date,
            String grouping
    ) {
        return "month".equals(grouping)
                ? date.withDayOfMonth(1)
                : date;
    }

    private LocalDate nextBucket(
            LocalDate date,
            String grouping
    ) {
        return "month".equals(grouping)
                ? date.plusMonths(1)
                : date.plusDays(1);
    }

    private BigDecimal percentage(
            BigDecimal amount,
            BigDecimal maximum
    ) {
        if (maximum.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return amount.multiply(BigDecimal.valueOf(100))
                .divide(
                        maximum,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void requireAdmin(String username) {
        if (username == null
                || accounts.paymentAdmin(username).isEmpty()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xem báo cáo doanh thu."
            );
        }
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static class Totals {

        private BigDecimal counterRevenue = BigDecimal.ZERO;
        private BigDecimal onlineRevenue = BigDecimal.ZERO;

        private long counterCount;
        private long onlineCount;
    }
}