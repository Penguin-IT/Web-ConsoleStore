package com.example.consolestore.DTO;

import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Entity.SerialMay;
import com.example.consolestore.Repository.HoaDonRepository;

import java.util.List;
import java.util.Map;

public final class OnlineOrderData {

    private OnlineOrderData() {
    }

    public record Detail(
            HoaDon invoice,
            HoaDonRepository.PaymentInfo payment,
            List<HoaDonRepository.OnlineLine> lines,
            Map<Long, List<SerialMay>> serials
    ) {
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "Chưa có thanh toán";
        }

        return switch (status) {
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "DA_XAC_NHAN" -> "Đã xác nhận";
            case "DANG_GIAO" -> "Đang giao";
            case "HOAN_TAT" -> "Hoàn tất";
            case "HUY" -> "Đã hủy";
            case "DA_THANH_TOAN" -> "Đã thanh toán";
            case "CHUA_THANH_TOAN" -> "Chưa thanh toán";
            default -> status;
        };
    }
}