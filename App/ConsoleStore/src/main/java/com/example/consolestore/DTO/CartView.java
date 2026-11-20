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
            BigDecimal price,       // giá thực tế (đã gồm mua kèm và hạng)
            BigDecimal listPrice,   // giá niêm yết
            boolean bundle,         // có được giá mua kèm không
            int quantity,
            long stock,
            boolean available,
            String warning
    ) {

        public BigDecimal getSubtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }

        public BigDecimal getSaved() {
            return listPrice.subtract(price)
                    .multiply(BigDecimal.valueOf(quantity));
        }
    }
}