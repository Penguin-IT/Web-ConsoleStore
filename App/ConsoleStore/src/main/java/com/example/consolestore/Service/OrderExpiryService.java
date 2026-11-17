package com.example.consolestore.Service;

import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Repository.HoaDonRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderExpiryService {

    private final HoaDonRepository invoices;

    public OrderExpiryService(HoaDonRepository invoices) {
        this.invoices = invoices;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            timeout = 15
    )
    public boolean cancelOne(
            Long invoiceId,
            int timeoutMinutes
    ) {
        HoaDon invoice = invoices.lockOnlineOrder(invoiceId)
                .orElse(null);

        if (invoice == null
                || !"CHO_XAC_NHAN".equals(invoice.getStatus())) {
            return false;
        }

        int updated = invoices.cancelExpiredOnlineOrder(
                invoiceId,
                timeoutMinutes
        );

        if (updated == 0) {
            return false;
        }

        if (updated != 1) {
            throw new IllegalStateException(
                    "Unexpected number of cancelled orders: " + updated
            );
        }

        return true;
    }
}
