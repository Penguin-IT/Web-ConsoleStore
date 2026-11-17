package com.example.consolestore.Service;

import com.example.consolestore.Dto.CartView;
import com.example.consolestore.Dto.ProductCard;
import com.example.consolestore.Dto.ProductDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CatalogService catalogService;

    public CartService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public Map<Long, Integer> add(
            Map<Long, Integer> current,
            Long productId,
            int quantity
    ) {
        validateQuantity(quantity);

        ProductCard product = requireProduct(productId);

        long nextQuantity =
                (long) current.getOrDefault(productId, 0) + quantity;

        validateStock(product, nextQuantity);

        Map<Long, Integer> updated = new LinkedHashMap<>(current);

        updated.put(productId, (int) nextQuantity);

        return updated;
    }

    public Map<Long, Integer> update(
            Map<Long, Integer> current,
            Long productId,
            int quantity
    ) {
        validateQuantity(quantity);

        if (!current.containsKey(productId)) {
            throw new IllegalArgumentException(
                    "Sản phẩm không có trong giỏ hàng."
            );
        }

        ProductCard product = requireProduct(productId);

        validateStock(product, quantity);

        Map<Long, Integer> updated = new LinkedHashMap<>(current);

        updated.put(productId, quantity);

        return updated;
    }

    public Map<Long, Integer> remove(
            Map<Long, Integer> current,
            Long productId
    ) {
        Map<Long, Integer> updated = new LinkedHashMap<>(current);

        updated.remove(productId);

        return updated;
    }

    public CartView view(Map<Long, Integer> items) {
        List<CartView.Line> lines = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            var detail = catalogService.detail(productId);

            if (detail.isEmpty()) {
                lines.add(new CartView.Line(
                        productId,
                        "Sản phẩm #" + productId,
                        "/images/ui/product-placeholder.svg",
                        BigDecimal.ZERO,
                        quantity,
                        0,
                        false,
                        "Sản phẩm đã ngừng kinh doanh. Vui lòng xóa khỏi giỏ."
                ));

                continue;
            }

            ProductCard product = detail.get().product();

            long stock = Math.max(0, product.stock());
            boolean available = quantity <= stock;

            String warning = "";

            if (stock == 0) {
                warning = "Sản phẩm hiện đã hết hàng.";
            } else if (!available) {
                warning = "Chỉ còn " + stock
                        + " sản phẩm. Vui lòng giảm số lượng.";
            }

            lines.add(new CartView.Line(
                    product.id(),
                    product.name(),
                    product.image(),
                    product.price(),
                    quantity,
                    stock,
                    available,
                    warning
            ));
        }

        long totalQuantity = lines.stream()
                .mapToLong(CartView.Line::quantity)
                .sum();

        BigDecimal totalAmount = lines.stream()
                .map(CartView.Line::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean readyToCheckout = !lines.isEmpty()
                && lines.stream().allMatch(CartView.Line::available);

        return new CartView(
                List.copyOf(lines),
                totalQuantity,
                totalAmount,
                readyToCheckout
        );
    }

    private ProductCard requireProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "Mã sản phẩm không hợp lệ."
            );
        }

        return catalogService.detail(productId)
                .map(ProductDetail::product)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh."
                ));
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng phải là số nguyên lớn hơn 0."
            );
        }
    }

    private void validateStock(
            ProductCard product,
            long quantity
    ) {
        if (product.stock() <= 0) {
            throw new IllegalArgumentException(
                    "Sản phẩm hiện đã hết hàng."
            );
        }

        if (quantity > Integer.MAX_VALUE
                || quantity > product.stock()) {
            throw new IllegalArgumentException(
                    "Số lượng trong giỏ không được vượt quá tồn kho. "
                            + "Hiện còn " + product.stock() + " sản phẩm."
            );
        }
    }
}