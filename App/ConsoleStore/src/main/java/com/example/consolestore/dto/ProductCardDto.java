package com.example.consolestore.dto;

public record ProductCardDto(
        String badge,
        String artClass,
        String artShape,
        String category,
        String name,
        String description,
        String priceText
) {}