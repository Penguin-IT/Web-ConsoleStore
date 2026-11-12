package com.example.consolestore.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThanhToan")
public class ThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maTT")
    private Long maTT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maHD", nullable = false)
    private HoaDon hoaDon;

    @Column(name = "phuongThuc", nullable = false, length = 20)
    private String phuongThuc;          // TIEN_MAT | CHUYEN_KHOAN | COD

    @Column(name = "soTien", nullable = false)
    private BigDecimal soTien;

    @Column(name = "trangThai", nullable = false, length = 20)
    private String trangThai = "CHUA_THANH_TOAN";

    @Column(name = "ngayThanhToan")
    private LocalDateTime ngayThanhToan;

    public Long getMaTT() {
        return maTT;
    }

    public void setMaTT(Long maTT) {
        this.maTT = maTT;
    }

    public HoaDon getHoaDon() {
        return hoaDon;
    }

    public void setHoaDon(HoaDon hoaDon) {
        this.hoaDon = hoaDon;
    }

    public String getPhuongThuc() {
        return phuongThuc;
    }

    public void setPhuongThuc(String phuongThuc) {
        this.phuongThuc = phuongThuc;
    }

    public BigDecimal getSoTien() {
        return soTien;
    }

    public void setSoTien(BigDecimal soTien) {
        this.soTien = soTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }
}