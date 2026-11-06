package com.example.consolestore.service;

import com.example.consolestore.dto.ProductCardDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomePageService {

    public List<ProductCardDto> getFeaturedProducts() {
        return List.of(
                new ProductCardDto(
                        "NỔI BẬT", "", "mini-console",
                        "CONSOLE · HÀNG MỚI",
                        "Console Series X",
                        "Hiệu năng mạnh mẽ, trải nghiệm đắm chìm.",
                        "12.990.000₫"
                ),
                new ProductCardDto(
                        "GỌN NHẸ", "blue", "mini-console white",
                        "CONSOLE · HÀNG MỚI",
                        "Console Series S",
                        "Thiết kế nhỏ gọn, sẵn sàng cho mọi không gian.",
                        "7.490.000₫"
                ),
                new ProductCardDto(
                        "PHỤ KIỆN", "orange", "gamepad",
                        "PHỤ KIỆN · TAY CẦM",
                        "Tay cầm không dây",
                        "Thoải mái điều khiển, kết nối cùng bạn bè.",
                        "1.590.000₫"
                )
        );
    }
}