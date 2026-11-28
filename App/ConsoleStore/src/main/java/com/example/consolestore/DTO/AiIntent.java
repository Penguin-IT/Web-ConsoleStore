package com.example.consolestore.DTO;

/**
 * Điều kiện lọc rút ra từ câu khách gõ.
 * Trường nào null là khách không nhắc tới. Giá tính theo VND.
 */
public record AiIntent(
        Long minPrice,
        Long maxPrice,
        String category,
        String platform,
        String condition,   // MOI | CU
        String keyword,
        String sort         // priceAsc | priceDesc
) {
    public boolean hasPrice() {
        return minPrice != null || maxPrice != null;
    }

    public boolean hasFilter() {
        return hasPrice() || category != null || platform != null
                || condition != null || keyword != null;
    }

    public AiIntent withoutPrice() {
        return new AiIntent(null, null, category, platform, condition, keyword, sort);
    }

    public AiIntent withoutKeyword() {
        return new AiIntent(minPrice, maxPrice, category, platform, condition, null, sort);
    }
}
