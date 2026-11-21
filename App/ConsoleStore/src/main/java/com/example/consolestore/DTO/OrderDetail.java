package com.example.consolestore.DTO;

import com.example.consolestore.Repository.HoaDonRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetail(
        HoaDonRepository.CustomerOrderHeader header,
        List<HoaDonRepository.CustomerOrderLine> lines
) {

    public String getStatusLabel() {
        if (header == null) return "";
        return switch (header.getStatus()) {
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "DA_XAC_NHAN"  -> "Đã xác nhận";
            case "DANG_GIAO"    -> "Đang giao";
            case "HOAN_TAT"     -> "Hoàn tất";
            case "HUY"          -> "Đã hủy";
            default             -> header.getStatus();
        };
    }

    public String getChannelLabel() {
        if (header == null) return "";
        return "ONLINE".equals(header.getChannel()) ? "Online" : "Tại quầy";
    }

    public String getPaymentLabel() {
        if (header == null) return "";
        if ("DA_THANH_TOAN".equals(header.getPaymentStatus())) return "Đã thanh toán";
        if ("HUY".equals(header.getStatus())) return "Không cần thanh toán";
        return header.getPaymentStatus() == null ? "Chưa có giao dịch" : "Chưa thanh toán";
    }

    public String getPaymentMethodLabel() {
        String m = header == null ? null : header.getPaymentMethod();
        if (m == null) return "—";
        return switch (m) {
            case "TIEN_MAT" -> "ONLINE".equals(header.getChannel())
                    ? "Thanh toán khi nhận hàng (COD)"
                    : "Tiền mặt";
            case "CHUYEN_KHOAN"  -> "Chuyển khoản ngân hàng";
            case "MOMO"          -> "MoMo";
            case "ZALOPAY"       -> "ZaloPay";
            default              -> m;
        };
    }

    public boolean isCancelable() {
        if (header == null) return false;
        return "ONLINE".equals(header.getChannel())
                && "CHO_XAC_NHAN".equals(header.getStatus())
                && (header.getPaymentStatus() == null
                    || "CHUA_THANH_TOAN".equals(header.getPaymentStatus()));
    }

    public boolean isPayable() {
        if (header == null) {
            return false;
        }

        String method = header.getPaymentMethod();

        return "ONLINE".equals(header.getChannel())
                && "CHO_XAC_NHAN".equals(header.getStatus())
                && "CHUA_THANH_TOAN".equals(header.getPaymentStatus())
                && method != null
                && !"TIEN_MAT".equals(method);
    }
}
