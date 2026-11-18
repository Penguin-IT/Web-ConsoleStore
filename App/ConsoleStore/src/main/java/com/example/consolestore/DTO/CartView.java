package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        List<Line> lines,
        long totalQuantity,
        BigDecimal totalAmount,
        boolean readyToCheckout
) {

    public record Line(
            Long productId,
            String name,
            String image,
            BigDecimal price,
            int quantity,
            long stock,
            boolean available,
            String warning
    ) {

        public BigDecimal getSubtotal() {
            return price.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }
}