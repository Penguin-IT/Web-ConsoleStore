package com.example.consolestore.Service;

import com.example.consolestore.DTO.BundleOffer;
import com.example.consolestore.DTO.CartView;
import com.example.consolestore.DTO.ProductCard;
import com.example.consolestore.DTO.ProductDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CatalogService catalogService;

    public CartService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // =========================================================
    // Thêm / sửa / xóa sản phẩm trong giỏ
    // =========================================================

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

    // =========================================================
    // Dựng giỏ hàng để hiển thị (đã tính giá mua kèm + hạng)
    // =========================================================

    public CartView view(Map<Long, Integer> items, int memberPercent) {
        // Lượt 1: đọc sản phẩm để biết trong giỏ có console của nền tảng nào
        Map<Long, ProductCard> found = new LinkedHashMap<>();

        for (Long productId : items.keySet()) {
            catalogService.detail(productId)
                    .ifPresent(d -> found.put(productId, d.product()));
        }

        Set<String> consolePlatforms = BundlePricing.consolePlatforms(
                found.values().stream()
                        .map(p -> new BundlePricing.Item(
                                p.platform(),
                                p.category()
                        ))
                        .toList()
        );

        // Lượt 2: dựng từng dòng với giá thực tế
        List<CartView.Line> lines = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            ProductCard product = found.get(productId);

            // Sản phẩm đã ngừng kinh doanh
            if (product == null) {
                lines.add(new CartView.Line(
                        productId,
                        "Sản phẩm #" + productId,
                        "/images/ui/product-placeholder.svg",
                        BigDecimal.ZERO,   // price
                        BigDecimal.ZERO,   // listPrice
                        false,             // bundle
                        quantity,
                        0,
                        false,
                        "Sản phẩm đã ngừng kinh doanh. Vui lòng xóa khỏi giỏ."
                ));

                continue;
            }

            // Sản phẩm bình thường
            long stock = Math.max(0, product.stock());
            boolean available = quantity <= stock;

            String warning = "";

            if (stock == 0) {
                warning = "Sản phẩm hiện đã hết hàng.";
            } else if (!available) {
                warning = "Chỉ còn " + stock
                        + " sản phẩm. Vui lòng giảm số lượng.";
            }

            var bundleItem = new BundlePricing.Item(
                    product.platform(),
                    product.category()
            );

            boolean bundle =
                    BundlePricing.eligible(bundleItem, consolePlatforms);

            // Giá cuối = giá niêm yết -> trừ mua kèm -> trừ ưu đãi hạng
            BigDecimal base = bundle
                    ? BundlePricing.bundlePrice(product.price())
                    : product.price();

            BigDecimal price =
                    BundlePricing.applyPercent(base, memberPercent);

            lines.add(new CartView.Line(
                    product.id(),
                    product.name(),
                    product.image(),
                    price,             // giá thực tế
                    product.price(),   // giá niêm yết
                    bundle,
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

    // =========================================================
    // Gợi ý mua kèm theo từng máy console trong giỏ
    // key = mã console, value = các sản phẩm gợi ý
    // =========================================================

    public Map<Long, List<BundleOffer>> bundleOffersByConsole(
            Map<Long, Integer> items,
            int limit
    ) {
        Map<Long, List<BundleOffer>> result = new LinkedHashMap<>();
        Set<Long> shown = new HashSet<>();   // mỗi gợi ý chỉ hiện một lần

        for (Long productId : items.keySet()) {
            ProductCard console = catalogService.detail(productId)
                    .map(ProductDetail::product)
                    .orElse(null);

            if (console == null) {
                continue;
            }

            Set<String> platforms = BundlePricing.consolePlatforms(List.of(
                    new BundlePricing.Item(
                            console.platform(),
                            console.category()
                    )
            ));

            // Không phải console, hoặc không rõ nền tảng
            if (platforms.isEmpty()) {
                continue;
            }

            List<BundleOffer> offers = new ArrayList<>();

            for (ProductCard candidate
                    : catalogService.related(console.id(), 12)) {

                var item = new BundlePricing.Item(
                        candidate.platform(),
                        candidate.category()
                );

                if (items.containsKey(candidate.id())
                        || candidate.stock() <= 0
                        || !BundlePricing.eligible(item, platforms)
                        || !shown.add(candidate.id())) {
                    continue;
                }

                BigDecimal bundle =
                        BundlePricing.bundlePrice(candidate.price());

                offers.add(new BundleOffer(
                        candidate,
                        bundle,
                        candidate.price().subtract(bundle)
                ));

                if (offers.size() >= limit) {
                    break;
                }
            }

            if (!offers.isEmpty()) {
                result.put(console.id(), List.copyOf(offers));
            }
        }

        return result;
    }

    // =========================================================
    // Hàm kiểm tra dùng nội bộ
    // =========================================================

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