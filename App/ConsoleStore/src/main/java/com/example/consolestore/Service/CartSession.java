package com.example.consolestore.Service;

import com.example.consolestore.DTO.CheckoutQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log =
            LoggerFactory.getLogger(CartSession.class);

    private String ownerKey;
    private Map<Long, Integer> items = Map.of();
    private CheckoutQuote quote;

    private final LinkedHashMap<String, Long> completed =
            new LinkedHashMap<>();

    // Gán lại mỗi lần vào locked(), không lưu theo session
    private transient CartStore store;
    private transient CartStore.Owner owner;

    public synchronized <T> T locked(
            CartStore.Owner owner,
            CartStore store,
            Supplier<T> operation
    ) {
        if (!Objects.equals(ownerKey, owner.key())) {
            ownerKey = owner.key();
            quote = null;
            completed.clear();
        }

        this.owner = owner;
        this.store = store;

        // DB là nguồn sự thật: luôn đọc lại (đổi thiết bị vẫn đúng)
        this.items = Collections.unmodifiableMap(
                new LinkedHashMap<>(store.load(owner))
        );

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

        Map<Long, Integer> next = Collections.unmodifiableMap(
                new LinkedHashMap<>(updated)
        );

        // Ghi DB trước; nếu lỗi thì giỏ trong session không đổi
        store.save(owner, next);

        items = next;
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

        // Đơn đã tạo xong; lỗi dọn giỏ không được làm hỏng luồng đặt hàng
        try {
            store.save(owner, remaining);
        } catch (RuntimeException exception) {
            log.warn(
                    "Không cập nhật được giỏ hàng sau đơn {}",
                    requestId,
                    exception
            );
        }
    }

    private void requireLock() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException(
                    "Use CartSession.locked()"
            );
        }
    }
}