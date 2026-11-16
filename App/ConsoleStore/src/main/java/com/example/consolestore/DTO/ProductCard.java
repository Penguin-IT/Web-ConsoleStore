package com.example.consolestore.DTO;

import java.math.BigDecimal;

/** Du lieu san pham can hien thi tren the giao dien. */
public record ProductCard(Long id,String name,String platform,String category,
                          BigDecimal price,String image,String condition,long stock) {}
