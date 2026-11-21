package com.example.consolestore.Service;

import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    public record Status(
            MemberTier tier,
            BigDecimal spent,
            MemberTier next,
            BigDecimal remaining
    ) {
        public int getPercent() {
            return tier.getPercent();
        }

        /** Phần trăm tiến độ tới hạng kế tiếp (0-100). */
        public int getProgress() {
            if (next == null) {
                return 100;
            }

            BigDecimal range = next.getThreshold().subtract(tier.getThreshold());
            BigDecimal done = spent.subtract(tier.getThreshold());

            int value = done.multiply(BigDecimal.valueOf(100))
                    .divide(range, 0, java.math.RoundingMode.DOWN)
                    .intValue();

            return Math.max(0, Math.min(100, value));
        }
    }

    private final TaiKhoanRepository accounts;
    private final HoaDonRepository invoices;

    public MemberService(
            TaiKhoanRepository accounts,
            HoaDonRepository invoices
    ) {
        this.accounts = accounts;
        this.invoices = invoices;
    }

    public List<MemberTier> tiers() {
        return Arrays.asList(MemberTier.values());
    }

    public int maxPercent() {
        return Arrays.stream(MemberTier.values())
                .mapToInt(MemberTier::getPercent)
                .max()
                .orElse(0);
    }

    public int bundlePercent() {
        return BundlePricing.PERCENT;
    }

    public Status status(String username) {
        BigDecimal spent = BigDecimal.ZERO;

        if (username != null) {
            BigDecimal value = accounts.findWithCustomer(username)
                    .filter(a -> "KHACHHANG".equals(a.getRole())
                            && a.getCustomer() != null)
                    .map(a -> invoices.completedSpend(a.getCustomer().getId()))
                    .orElse(null);

            if (value != null) {
                spent = value;
            }
        }

        MemberTier tier = MemberTier.forSpent(spent);
        MemberTier next = tier.next();

        BigDecimal remaining = next == null
                ? BigDecimal.ZERO
                : next.getThreshold().subtract(spent);

        return new Status(tier, spent, next, remaining);
    }
}