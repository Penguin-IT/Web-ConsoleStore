package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GuestCartCleanupJob {

    private static final Logger log =
            LoggerFactory.getLogger(GuestCartCleanupJob.class);

    private final CartStore store;

    public GuestCartCleanupJob(CartStore store) {
        this.store = store;
    }

    @Scheduled(cron = "0 30 3 * * *")   // 03:30 mỗi ngày
    public void run() {
        int removed = store.purgeStaleGuestCarts(30);

        if (removed > 0) {
            log.info("Đã xóa {} giỏ hàng vãng lai quá hạn", removed);
        }
    }
}