package com.example.consolestore.DTO;

import java.util.List;

/** Kết quả trả về cho khung chat AI. */
public record AiChatResponse(
        String reply,
        List<ProductCard> products,
        String moreUrl          // link "Xem tất cả" tới trang /products (có thể null)
) {}
