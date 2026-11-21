package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.util.List;
import com.example.consolestore.Service.BundlePricing;

public record CartView(
        List<Line> lines,
        long totalQuantity,
        BigDecimal totalAmount,
        boolean readyToCheckout
) {
    public BigDecimal getListTotal() {
        return lines.stream()
                .map(l -> l.listPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBundleSaved() {
        return lines.stream().map(Line::getBundleSaved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getMemberSaved() {
        return getSaved().subtract(getBundleSaved());
    }
    public BigDecimal getSaved() {
        return lines.stream()
                .map(Line::getSaved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
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
        public BigDecimal getBundleSaved() {
            if (!bundle) return BigDecimal.ZERO;
            return listPrice.subtract(BundlePricing.bundlePrice(listPrice))
                    .multiply(BigDecimal.valueOf(quantity));
        }
        public BigDecimal getSubtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }

        public BigDecimal getSaved() {
            return listPrice.subtract(price)
                    .multiply(BigDecimal.valueOf(quantity));
        }
    }
}