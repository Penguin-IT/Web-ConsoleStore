package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaGiamGia")
public class MaGiamGia {
    @Id @Column(name = "maCode", length = 30)
    private String code;

    @Nationalized @Column(name = "moTa", length = 200)
    private String description;

    @Column(name = "loai", length = 10)
    private String type;

    @Column(name = "giaTri", precision = 18, scale = 0)
    private BigDecimal value;

    @Column(name = "giamToiDa", precision = 18, scale = 0)
    private BigDecimal maxDiscount;

    @Column(name = "donToiThieu", precision = 18, scale = 0)
    private BigDecimal minOrder;

    @Column(name = "soLuong")
    private Integer quantity;

    @Column(name = "moiKhachToiDa")
    private int perCustomer;

    @Column(name = "batDau", columnDefinition = "datetime")
    private LocalDateTime startsAt;

    @Column(name = "ketThuc", columnDefinition = "datetime")
    private LocalDateTime endsAt;

    @Column(name = "hienTrangChu")
    private boolean featured;

    @Column(name = "hoatDong")
    private boolean active;

    protected MaGiamGia() {}

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public BigDecimal getMinOrder() { return minOrder; }
    public Integer getQuantity() { return quantity; }
    public int getPerCustomer() { return perCustomer; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public boolean isFeatured() { return featured; }
    public boolean isActive() { return active; }
}