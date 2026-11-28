package com.example.consolestore.Service;

import com.example.consolestore.Repository.HoaDonRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "store.order-expiry",
        name = "enabled",
        havingValue = "true"
)
public class OrderExpiryJob {

    private static final Logger log =
            LoggerFactory.getLogger(OrderExpiryJob.class);

    private final HoaDonRepository invoices;
    private final OrderExpiryService expiryService;

    private final int timeoutMinutes;
    private final int batchSize;
    private final int codTimeoutMinutes;
    public OrderExpiryJob(
            HoaDonRepository invoices,
            OrderExpiryService expiryService,
            @Value("${store.order-expiry.timeout-minutes:30}")
            int timeoutMinutes,
            @Value("${store.order-expiry.cod-timeout-minutes:1440}")
            int codTimeoutMinutes,
            @Value("${store.order-expiry.batch-size:100}")
            int batchSize
    ) {
        if (timeoutMinutes < 1 || timeoutMinutes > 525600) {
            throw new IllegalArgumentException(
                    "Order expiry timeout must be between "
                            + "1 and 525600 minutes."
            );
        }

        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException(
                    "Order expiry batch size must be between 1 and 500."
            );
        }

        this.invoices = invoices;
        this.expiryService = expiryService;
        this.timeoutMinutes = timeoutMinutes;
        this.batchSize = batchSize;
        this.codTimeoutMinutes = codTimeoutMinutes;
    }

    @Scheduled(
            scheduler = "orderExpiryScheduler",
            initialDelayString =
                    "${store.order-expiry.initial-delay-ms:60000}",
            fixedDelayString =
                    "${store.order-expiry.scan-delay-ms:60000}"
    )
    public void scan() {
        String thread = Thread.currentThread().getName();

        List<Long> candidates;

        try {
            candidates = invoices.findExpiredOnlineOrderIds(
                    timeoutMinutes,
                    codTimeoutMinutes,
                    batchSize
            );
        } catch (RuntimeException exception) {
            log.error(
                    "CN23 scan failed, thread={}",
                    thread,
                    exception
            );

            return;
        }

        int cancelled = 0;
        int skipped = 0;
        int failed = 0;

        for (Long invoiceId : candidates) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                boolean changed = expiryService.cancelOne(
                        invoiceId,
                        timeoutMinutes
                );

                if (changed) {
                    cancelled++;

                    log.info(
                            "CN23 cancelled expired order HD{}, thread={}",
                            invoiceId,
                            thread
                    );
                } else {
                    skipped++;

                    log.debug(
                            "CN23 skipped order HD{}, thread={}",
                            invoiceId,
                            thread
                    );
                }
            } catch (RuntimeException exception) {
                failed++;

                log.error(
                        "CN23 cannot cancel order HD{}, thread={}",
                        invoiceId,
                        thread,
                        exception
                );
            }
        }

        log.info(
                "CN23 scan completed: candidates={}, "
                        + "cancelled={}, skipped={}, failed={}, thread={}",
                candidates.size(),
                cancelled,
                skipped,
                failed,
                thread
        );
    }
}