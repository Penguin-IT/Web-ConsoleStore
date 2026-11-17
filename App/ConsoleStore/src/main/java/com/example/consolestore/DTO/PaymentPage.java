package com.example.consolestore.Dto;

import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Repository.HoaDonRepository;

import java.util.List;

public record PaymentPage(
        HoaDon invoice,
        HoaDonRepository.PaymentInfo payment,
        List<HoaDonRepository.LineInfo> lines
) {

    public String getMethodLabel() {
        return switch (payment.getMethod()) {
            case "CHUYEN_KHOAN" -> "Chuyển khoản ngân hàng";
            case "MOMO" -> "Ví MoMo";
            case "ZALOPAY" -> "Ví ZaloPay";
            default -> "Không xác định";
        };
    }
}