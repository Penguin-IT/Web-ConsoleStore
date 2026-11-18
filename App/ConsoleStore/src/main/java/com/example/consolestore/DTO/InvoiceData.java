package com.example.consolestore.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class InvoiceData {

    private InvoiceData() {
    }

    public record Header(
            Long id,
            LocalDateTime createdAt,
            String customerName,
            String phone,
            String address,
            String employeeName,
            String warehouseName,
            String channel,
            String status,
            BigDecimal totalAmount,
            String note
    ) {

        public String channelLabel() {
            return "QUAY".equals(channel)
                    ? "Tại quầy"
                    : "Online";
        }

        public String statusLabel() {
            return switch (status) {
                case "CHO_XAC_NHAN" -> "Chờ xác nhận";
                case "DA_XAC_NHAN" -> "Đã xác nhận";
                case "DANG_GIAO" -> "Đang giao";
                case "HOAN_TAT" -> "Hoàn tất";
                case "HUY" -> "Đã hủy";
                default -> status;
            };
        }
    }

    public record Line(
            Long productId,
            String productName,
            String condition,
            String serial,
            String warehouse,
            int quantity,
            BigDecimal price,
            BigDecimal subtotal
    ) {
    }

    public record Payment(
            Long id,
            String method,
            BigDecimal amount,
            String status,
            LocalDateTime paidAt
    ) {

        public String methodLabel() {
            return switch (method) {
                case "TIEN_MAT" -> "Tiền mặt";
                case "CHUYEN_KHOAN" -> "Chuyển khoản";
                case "MOMO" -> "MoMo";
                case "ZALOPAY" -> "ZaloPay";
                default -> method;
            };
        }

        public String statusLabel() {
            return "DA_THANH_TOAN".equals(status)
                    ? "Đã thanh toán"
                    : "Chưa thanh toán";
        }
    }

    public record Detail(
            Header invoice,
            List<Line> lines,
            List<Payment> payments
    ) {

        public BigDecimal paidAmount() {
            return payments.stream()
                    .filter(payment ->
                            "DA_THANH_TOAN".equals(payment.status())
                    )
                    .map(Payment::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}