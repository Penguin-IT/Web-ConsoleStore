package com.example.consolestore.Entity;

import jakarta.persistence.*;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "BienDongKho")
public class BienDongKho {

    @Id
    @Column(name = "maBD")
    private Long id;

    @Nationalized
    @Column(name = "loaiBienDong", length = 20)
    private String type;

    @Column(name = "soLuong")
    private int quantity;

    @Column(name = "soLuongTruoc")
    private int beforeQuantity;

    @Column(name = "soLuongSau")
    private int afterQuantity;

    @Column(name = "thoiGian", columnDefinition = "datetime")
    private LocalDateTime occurredAt;

    @Column(name = "maKho")
    private Long warehouseId;

    @Column(name = "maSP")
    private Long productId;

    @Column(name = "maNV")
    private Long employeeId;

    @Column(name = "maCTPN")
    private Long purchaseLineId;

    @Column(name = "maCTHD")
    private Long invoiceLineId;

    @Column(name = "maCTTC")
    private Long tradeInLineId;

    protected BienDongKho() {
    }

    public Long getId() {
        return id;
    }
}