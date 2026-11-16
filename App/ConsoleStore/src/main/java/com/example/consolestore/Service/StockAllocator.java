package com.example.consolestore.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class StockAllocator {

    private StockAllocator() {
    }

    public record Stock(
            long warehouseId,
            long productId,
            long quantity
    ) {
    }

    public record Allocation(
            long warehouseId,
            long productId,
            int quantity
    ) {
    }

    public static List<Allocation> allocate(
            Map<Long, Integer> requested,
            List<Stock> stocks
    ) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng đang trống."
            );
        }

        for (var entry : requested.entrySet()) {
            if (entry.getKey() == null
                    || entry.getKey() <= 0
                    || entry.getValue() == null
                    || entry.getValue() <= 0) {
                throw new IllegalArgumentException(
                        "Sản phẩm hoặc số lượng không hợp lệ."
                );
            }
        }

        Map<Long, List<Stock>> grouped = new HashMap<>();

        for (Stock stock : stocks) {
            if (stock.quantity() > 0) {
                grouped.computeIfAbsent(
                        stock.productId(),
                        key -> new ArrayList<>()
                ).add(stock);
            }
        }

        List<Allocation> result = new ArrayList<>();

        for (var entry : new TreeMap<>(requested).entrySet()) {
            long remaining = entry.getValue();

            List<Stock> available = grouped
                    .getOrDefault(entry.getKey(), List.of())
                    .stream()
                    .sorted(
                            Comparator.comparingLong(
                                    Stock::warehouseId
                            )
                    )
                    .toList();

            for (Stock stock : available) {
                if (remaining == 0) {
                    break;
                }

                int quantity = (int) Math.min(
                        remaining,
                        stock.quantity()
                );

                result.add(new Allocation(
                        stock.warehouseId(),
                        entry.getKey(),
                        quantity
                ));

                remaining -= quantity;
            }

            if (remaining > 0) {
                long availableQuantity =
                        entry.getValue() - remaining;

                throw new IllegalArgumentException(
                        "Sản phẩm #" + entry.getKey()
                                + " chỉ còn " + availableQuantity
                                + " sản phẩm trong tổng tồn."
                );
            }
        }

        return List.copyOf(result);
    }
}