package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.util.List;

public record RevenueReport(
        String period,
        String grouping,
        List<Row> rows,
        BigDecimal counterRevenue,
        BigDecimal onlineRevenue,
        long counterCount,
        long onlineCount,
        BigDecimal chartMaximum
) {

    public BigDecimal totalRevenue() {
        return counterRevenue.add(onlineRevenue);
    }

    public long invoiceCount() {
        return counterCount + onlineCount;
    }

    public record Row(
            String label,
            long counterCount,
            long onlineCount,
            BigDecimal counterRevenue,
            BigDecimal onlineRevenue,
            BigDecimal counterPercent,
            BigDecimal onlinePercent
    ) {

        public long invoiceCount() {
            return counterCount + onlineCount;
        }

        public BigDecimal totalRevenue() {
            return counterRevenue.add(onlineRevenue);
        }
    }
}