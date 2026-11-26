package com.example.consolestore.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chặn spam để không ai đốt hết tiền API của bạn.
 * Mỗi IP tối đa N câu/phút, và cả web tối đa 10N câu/phút.
 */
@Component
public class AiRateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final int perIp;
    private final int global;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public AiRateLimiter(@Value("${store.ai.rate-limit-per-minute:12}") int perIp) {
        this.perIp = perIp;
        this.global = perIp * 10;
    }

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();
        if (hits.size() > 5_000) hits.clear();              // tránh phình bộ nhớ
        return take("*", global, now) && take(ip, perIp, now);
    }

    private boolean take(String key, int limit, long now) {
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= limit) return false;
            q.addLast(now);
            return true;
        }
    }
}
