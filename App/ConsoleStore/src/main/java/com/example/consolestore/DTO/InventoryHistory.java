package com.example.consolestore.DTO;

import com.example.consolestore.Repository.BienDongKhoRepository;

import org.springframework.data.domain.Page;

public record InventoryHistory(
        Long productId,
        String productName,
        String condition,
        boolean deleted,
        long currentQuantity,
        Page<BienDongKhoRepository.MovementRow> movements
) {
}