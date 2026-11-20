package com.example.consolestore.Service;

import com.example.consolestore.Entity.MaGiamGia;
import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.MaGiamGiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PricingService {

    // ===== Luật phí vận chuyển: sửa số ở đây =====
    private static final BigDecimal FREE_SHIP_FROM = new BigDecimal("3000000"); // freeship từ
    private static final BigDecimal SHIP_HCM       = new BigDecimal("20000");
    private static final BigDecimal SHIP_OTHER     = new BigDecimal("40000");
    private static final BigDecimal MIN_PAYABLE    = new BigDecimal("1000");    // luôn phải trả tối thiểu

    public record Quote(
            BigDecimal goods,
            BigDecimal shipping,
            boolean shippingPending,   // chưa chọn tỉnh/thành
            BigDecimal discount,
            String couponCode,         // null nếu không áp mã
            boolean couponValid,
            String message
    ) {
        public BigDecimal getPayable() {
            return goods.add(shipping).subtract(discount);
        }

        /** Dữ liệu đã định dạng sẵn cho JS ở trang thanh toán. */
        public Map<String, Object> view() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("couponValid", couponValid);
            m.put("message", message);
            m.put("code", couponCode == null ? "" : couponCode);
            m.put("shipping", shippingPending ? "Chọn tỉnh/thành để tính"
                    : shipping.signum() == 0 ? "Miễn phí" : money(shipping));
            m.put("hasDiscount", discount.signum() > 0);
            m.put("discount", "-" + money(discount));
            m.put("payable", money(getPayable()));
            return m;
        }
    }

    private final MaGiamGiaRepository coupons;
    private final HoaDonRepository invoices;

    public PricingService(MaGiamGiaRepository coupons, HoaDonRepository invoices) {
        this.coupons = coupons;
        this.invoices = invoices;
    }

    // ---------- Dùng cho giao diện (trang chủ, giỏ hàng) ----------
    public BigDecimal freeShipFrom() { return FREE_SHIP_FROM; }
    public BigDecimal shipHcm()      { return SHIP_HCM; }
    public BigDecimal shipOther()    { return SHIP_OTHER; }

    public BigDecimal freeShipRemaining(BigDecimal goods) {
        return FREE_SHIP_FROM.subtract(goods).max(BigDecimal.ZERO);
    }

    public int freeShipPercent(BigDecimal goods) {
        return goods.multiply(BigDecimal.valueOf(100))
                .divide(FREE_SHIP_FROM, 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }

    @Transactional(readOnly = true)
    public List<MaGiamGia> featuredCoupons() {
        return coupons.featured(LocalDateTime.now());
    }

    public String headline(MaGiamGia c) {
        return switch (c.getType()) {
            case "PHAN_TRAM" -> "Giảm " + c.getValue().toPlainString() + "%"
                    + (c.getMaxDiscount() == null ? "" : " (tối đa " + money(c.getMaxDiscount()) + ")");
            case "SO_TIEN" -> "Giảm " + money(c.getValue());
            default -> "Miễn phí vận chuyển";
        };
    }

    public String expires(MaGiamGia c) {
        return "HSD " + c.getEndsAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String money(BigDecimal value) {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(value) + " ₫";
    }

    // ---------- Tính tiền ----------
    /** Xem trước ở trang thanh toán (không khóa). */
    @Transactional(readOnly = true)
    public Quote preview(BigDecimal goods, String province, String code, Long customerId) {
        String key = normalize(code);
        Optional<MaGiamGia> found = key.isEmpty() ? Optional.empty() : coupons.findById(key);
        return evaluate(goods, province, key, customerId, found);
    }

    /** Gọi bên trong transaction đặt hàng: khóa dòng mã để chống dùng vượt số lượng. */
    public Quote quoteForOrder(BigDecimal goods, String province, String code, Long customerId) {
        String key = normalize(code);
        Optional<MaGiamGia> found = key.isEmpty() ? Optional.empty() : coupons.lockByCode(key);
        return evaluate(goods, province, key, customerId, found);
    }

    private Quote evaluate(BigDecimal goods, String province, String code,
                           Long customerId, Optional<MaGiamGia> found) {
        boolean pending = province == null || province.isBlank();
        BigDecimal shipping = pending ? BigDecimal.ZERO : shippingFee(goods, province);

        if (code.isEmpty()) {
            return new Quote(goods, shipping, pending, BigDecimal.ZERO, null, false, "");
        }
        if (found.isEmpty()) {
            return bad(goods, shipping, pending, "Mã giảm giá không tồn tại.");
        }

        MaGiamGia c = found.get();
        LocalDateTime now = LocalDateTime.now();

        if (!c.isActive() || now.isBefore(c.getStartsAt()) || now.isAfter(c.getEndsAt())) {
            return bad(goods, shipping, pending, "Mã giảm giá đã hết hạn hoặc chưa đến thời gian áp dụng.");
        }
        if (goods.compareTo(c.getMinOrder()) < 0) {
            return bad(goods, shipping, pending, "Đơn tối thiểu " + money(c.getMinOrder()) + " để dùng mã này.");
        }
        if (c.getQuantity() != null && invoices.couponUses(c.getCode()) >= c.getQuantity()) {
            return bad(goods, shipping, pending, "Mã giảm giá đã hết lượt sử dụng.");
        }
        if (invoices.couponUsesByCustomer(c.getCode(), customerId) >= c.getPerCustomer()) {
            return bad(goods, shipping, pending, "Bạn đã dùng hết số lượt của mã này.");
        }

        boolean freeShipCoupon = "FREESHIP".equals(c.getType());

        BigDecimal discount = switch (c.getType()) {
            case "PHAN_TRAM" -> {
                BigDecimal d = goods.multiply(c.getValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                yield c.getMaxDiscount() == null ? d : d.min(c.getMaxDiscount());
            }
            case "SO_TIEN"  -> c.getValue();
            case "FREESHIP" -> shipping;
            default         -> BigDecimal.ZERO;
        };

        // Không giảm quá số tiền phải trả (trigger ThanhToan yêu cầu tổng > 0).
        discount = discount.min(goods.add(shipping).subtract(MIN_PAYABLE).max(BigDecimal.ZERO));

        if (discount.signum() <= 0) {
            if (freeShipCoupon && pending) {
                return new Quote(goods, shipping, true, BigDecimal.ZERO, c.getCode(), true,
                        "Ưu đãi freeship sẽ được tính sau khi bạn chọn tỉnh/thành.");
            }
            return bad(goods, shipping, pending, freeShipCoupon
                    ? "Đơn này đã được miễn phí vận chuyển."
                    : "Mã này không giảm thêm cho đơn hiện tại.");
        }

        return new Quote(goods, shipping, pending, discount, c.getCode(), true,
                "Đã áp dụng mã " + c.getCode() + ": -" + money(discount));
    }

    private static Quote bad(BigDecimal goods, BigDecimal shipping, boolean pending, String message) {
        return new Quote(goods, shipping, pending, BigDecimal.ZERO, null, false, message);
    }

    private static BigDecimal shippingFee(BigDecimal goods, String province) {
        if (goods.compareTo(FREE_SHIP_FROM) >= 0) return BigDecimal.ZERO;
        return isHcm(province) ? SHIP_HCM : SHIP_OTHER;
    }

    private static boolean isHcm(String province) {
        String s = Normalizer.normalize(province, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
        return s.contains("ho chi minh");
    }

    private static String normalize(String code) {
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }
}