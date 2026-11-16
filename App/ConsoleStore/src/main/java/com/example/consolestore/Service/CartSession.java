package com.example.consolestore.Service;

import com.example.consolestore.Dto.CheckoutQuote;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@SessionScope
public class CartSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String owner;
    private Map<Long, Integer> items = Map.of();
    private CheckoutQuote quote;

    private final LinkedHashMap<String, Long> completed =
            new LinkedHashMap<>();

    public synchronized <T> T locked(
            String username,
            Supplier<T> operation
    ) {
        if (!Objects.equals(owner, username)) {
            owner = username;
            items = Map.of();
            quote = null;
            completed.clear();
        }

        return operation.get();
    }

    public Map<Long, Integer> items() {
        requireLock();
        return items;
    }

    public CheckoutQuote currentQuote() {
        requireLock();
        return quote;
    }

    public Long completed(String requestId) {
        requireLock();
        return completed.get(requestId);
    }

    public void requireEditable() {
        requireLock();

        if (quote != null && quote.submitted()) {
            throw new IllegalArgumentException(
                    "Yêu cầu đặt hàng đang được kiểm tra. "
                            + "Vui lòng quay lại thanh toán."
            );
        }
    }

    public void replaceItems(Map<Long, Integer> updated) {
        requireEditable();

        items = Collections.unmodifiableMap(
                new LinkedHashMap<>(updated)
        );

        quote = null;
    }

    public CheckoutQuote prepareQuote(
            Map<Long, BigDecimal> prices
    ) {
        requireLock();

        if (quote != null && (
                quote.submitted()
                        || (
                        quote.items().equals(items)
                                && quote.prices().equals(prices)
                )
        )) {
            return quote;
        }

        quote = new CheckoutQuote(
                UUID.randomUUID().toString(),
                items,
                prices,
                false
        );

        return quote;
    }

    public void markSubmitted() {
        requireLock();

        if (quote == null) {
            throw new IllegalStateException(
                    "Checkout quote is missing"
            );
        }

        quote = new CheckoutQuote(
                quote.requestId(),
                quote.items(),
                quote.prices(),
                true
        );
    }

    public void rejectAttempt() {
        requireLock();
        quote = null;
    }

    public void complete(
            String requestId,
            Long invoiceId
    ) {
        requireLock();

        if (completed.containsKey(requestId)) {
            return;
        }

        if (quote == null
                || !quote.requestId().equals(requestId)) {
            throw new IllegalStateException(
                    "Checkout quote does not match"
            );
        }

        Map<Long, Integer> remaining =
                new LinkedHashMap<>(items);

        quote.items().forEach(
                (id, quantity) -> remaining.computeIfPresent(
                        id,
                        (key, current) -> current > quantity
                                ? current - quantity
                                : null
                )
        );

        items = Collections.unmodifiableMap(remaining);
        completed.put(requestId, invoiceId);

        if (completed.size() > 20) {
            completed.remove(
                    completed.keySet().iterator().next()
            );
        }

        quote = null;
    }

    private void requireLock() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException(
                    "Use CartSession.locked()"
            );
        }
    }
}