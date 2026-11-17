package com.example.consolestore.Dto;

import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public final class PosData {

    private PosData() {
    }

    public record Customer(
            Long id,
            String name,
            String phone,
            String address,
            boolean newCustomer
    ) implements Serializable {
    }

    public record Line(
            Long productId,
            String name,
            Long serialId,
            String serialNumber,
            int quantity,
            BigDecimal price
    ) implements Serializable {

        public BigDecimal getSubtotal() {
            return price.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }

    public record Quote(
            String requestId,
            Long employeeId,
            Long warehouseId,
            String warehouseName,
            Customer customer,
            Long tradeInId,
            String note,
            List<Line> lines
    ) implements Serializable {

        public Quote {
            lines = List.copyOf(lines);
        }

        public BigDecimal getTotal() {
            return lines.stream()
                    .map(Line::getSubtotal)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );
        }
    }

    public record Options(
            List<Kho> warehouses,
            Long warehouseId,
            List<SanPhamRepository.PosOption> products,
            List<SerialMay> serials
    ) {
    }

    public record Receipt(
            HoaDon invoice,
            HoaDonRepository.PaymentInfo payment,
            List<HoaDonRepository.PosInvoiceLine> lines,
            List<Long> tradeInIds
    ) {

        public String getMethodLabel() {
            return switch (payment.getMethod()) {
                case "TIEN_MAT" -> "Tiền mặt";
                case "CHUYEN_KHOAN" -> "Chuyển khoản ngân hàng";
                case "MOMO" -> "Ví MoMo";
                case "ZALOPAY" -> "Ví ZaloPay";
                default -> payment.getMethod();
            };
        }
    }
}