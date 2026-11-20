package com.example.consolestore.Service;

import java.math.BigDecimal;

public enum MemberTier {
    MEMBER("Thành viên", 0L, 0),
    SILVER("Bạc", 10_000_000L, 3),
    GOLD("Vàng", 30_000_000L, 5),
    DIAMOND("Kim cương", 80_000_000L, 8);

    private final String label;
    private final BigDecimal threshold;
    private final int percent;

    MemberTier(String label, long threshold, int percent) {
        this.label = label;
        this.threshold = BigDecimal.valueOf(threshold);
        this.percent = percent;
    }

    public String getLabel() { return label; }
    public BigDecimal getThreshold() { return threshold; }
    public int getPercent() { return percent; }

    /** Hạng ứng với tổng chi tiêu (null được coi là 0). */
    public static MemberTier forSpent(BigDecimal spent) {
        MemberTier result = MEMBER;

        if (spent == null) {
            return result;
        }

        for (MemberTier tier : values()) {
            if (spent.compareTo(tier.threshold) >= 0) {
                result = tier;
            }
        }

        return result;
    }

    /** Hạng kế tiếp, null nếu đã ở hạng cao nhất. */
    public MemberTier next() {
        MemberTier[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}