package com.example.consolestore.Dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CheckoutQuote(
        String requestId,
        Map<Long, Integer> items,
        Map<Long, BigDecimal> prices,
        boolean submitted
) implements Serializable {

    public CheckoutQuote {
        items = Collections.unmodifiableMap(
                new LinkedHashMap<>(items)
        );

        prices = Collections.unmodifiableMap(
                new LinkedHashMap<>(prices)
        );
    }
}