package com.example.consolestore.DTO;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public record StockReport(
        String period,
        List<BestSeller> bestSellers,
        Page<LowStock> lowStock,
        int threshold
) {

    public long topQuantity() {
        return bestSellers.stream()
                .mapToLong(BestSeller::quantity)
                .sum();
    }

    public record BestSeller(
            Long id,
            String name,
            String image,
            String condition,
            boolean deleted,
            long quantity,
            BigDecimal revenue
    ) {
    }

    public record LowStock(
            Long id,
            String name,
            String image,
            String category,
            String condition,
            long quantity
    ) {
    }
}