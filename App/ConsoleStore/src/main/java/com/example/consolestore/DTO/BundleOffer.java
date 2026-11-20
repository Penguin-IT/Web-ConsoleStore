package com.example.consolestore.DTO;

import java.math.BigDecimal;

public record BundleOffer(
        ProductCard product,
        BigDecimal bundlePrice,
        BigDecimal saved
) {}