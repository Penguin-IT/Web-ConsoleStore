package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(
        Long id,
        LocalDateTime createdAt,
        String channel,
        String status,
        BigDecimal totalAmount,
        String paymentStatus
) {

    public String getStatusLabel() {
        return switch (status) {
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "DA_XAC_NHAN" -> "Đã xác nhận";
            case "DANG_GIAO" -> "Đang giao";
            case "HOAN_TAT" -> "Hoàn tất";
            case "HUY" -> "Đã hủy";
            default -> status;
        };
    }

    public String getPaymentLabel() {
        if ("DA_THANH_TOAN".equals(paymentStatus)) {
            return "Đã thanh toán";
        }

        if ("HUY".equals(status)) {
            return "Không cần thanh toán";
        }

        return paymentStatus == null
                ? "Chưa có giao dịch"
                : "Chưa thanh toán";
    }

    public String getChannelLabel() {
        return "ONLINE".equals(channel)
                ? "Online"
                : "Tại quầy";
    }

    public boolean isCancelable() {
        return "ONLINE".equals(channel)
                && "CHO_XAC_NHAN".equals(status)
                && (
                paymentStatus == null
                        || "CHUA_THANH_TOAN".equals(paymentStatus)
        );
    }

    public boolean isPayable() {
        return "ONLINE".equals(channel)
                && "CHO_XAC_NHAN".equals(status)
                && "CHUA_THANH_TOAN".equals(paymentStatus);
    }
}