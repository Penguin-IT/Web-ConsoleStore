package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseCostReport(
        String period,
        List<MonthRow> months,
        List<SupplierRow> suppliers,
        long receiptCount,
        BigDecimal totalAmount
) {

    public record MonthRow(
            String label,
            long receiptCount,
            BigDecimal amount
    ) {
    }

    public record SupplierRow(
            Long id,
            String name,
            long receiptCount,
            BigDecimal amount
    ) {
    }
}