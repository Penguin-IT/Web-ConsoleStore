package com.example.consolestore.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuThuCu")
public class PhieuThuCu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maPTC")
    private Long maPTC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKH", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maHD")
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKho", nullable = false)
    private Kho kho;

    @Column(name = "ngayThu", nullable = false)
    private LocalDateTime ngayThu;

    @Column(name = "tongTienThu", nullable = false)
    private BigDecimal tongTienThu;

    @Column(name = "ghiChu", length = 255)
    private String ghiChu;

    @PrePersist
    void prePersist() {
        if (ngayThu == null) {
            ngayThu = LocalDateTime.now();
        }
        if (tongTienThu == null) {
            tongTienThu = BigDecimal.ZERO;
        }
    }

    public Long getMaPTC() {
        return maPTC;
    }

    public void setMaPTC(Long maPTC) {
        this.maPTC = maPTC;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public HoaDon getHoaDon() {
        return hoaDon;
    }

    public void setHoaDon(HoaDon hoaDon) {
        this.hoaDon = hoaDon;
    }

    public Kho getKho() {
        return kho;
    }

    public void setKho(Kho kho) {
        this.kho = kho;
    }

    public LocalDateTime getNgayThu() {
        return ngayThu;
    }

    public void setNgayThu(LocalDateTime ngayThu) {
        this.ngayThu = ngayThu;
    }

    public BigDecimal getTongTienThu() {
        return tongTienThu;
    }

    public void setTongTienThu(BigDecimal tongTienThu) {
        this.tongTienThu = tongTienThu;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
