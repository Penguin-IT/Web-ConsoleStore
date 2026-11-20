package com.example.consolestore.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class BundlePricing {

    public static final String CONSOLE = "Máy console";
    public static final String NO_PLATFORM = "Khác";
    public static final int PERCENT = 10;

    private BundlePricing() {}

    public record Item(String platform, String category) {}

    /** Các nền tảng có ít nhất một máy console trong giỏ. */
    public static Set<String> consolePlatforms(Collection<Item> items) {
        Set<String> result = new HashSet<>();

        for (Item item : items) {
            if (CONSOLE.equals(item.category())
                    && hasPlatform(item.platform())) {
                result.add(item.platform());
            }
        }

        return result;
    }

    public static boolean eligible(Item item, Set<String> consolePlatforms) {
        return !CONSOLE.equals(item.category())
                && hasPlatform(item.platform())
                && consolePlatforms.contains(item.platform());
    }
    public static BigDecimal applyPercent(BigDecimal price, int percent) {
        if (percent <= 0) {
            return price;
        }

        return price
                .multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
    /** Giá mua kèm, làm tròn về số nguyên (DB dùng DECIMAL(18,0)). */
    public static BigDecimal bundlePrice(BigDecimal listPrice) {
        return listPrice
                .multiply(BigDecimal.valueOf(100 - PERCENT))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    /** Giá bán thực tế của một sản phẩm trong giỏ. */
    public static BigDecimal unitPrice(
            Item item,
            BigDecimal listPrice,
            Set<String> consolePlatforms
    ) {
        return eligible(item, consolePlatforms)
                ? bundlePrice(listPrice)
                : listPrice;
    }

    private static boolean hasPlatform(String platform) {
        return platform != null
                && !platform.isBlank()
                && !NO_PLATFORM.equals(platform);
    }
}