package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HoaDon")
@DynamicUpdate
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maHD")
    private Long maHD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKH")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV")
    private NhanVien nhanVien;

    @Column(name = "maKho", nullable = false)
    private Long maKho;

    @Column(name = "ngayLap", nullable = false)
    private LocalDateTime ngayLap;

    @Column(name = "kenhBan", nullable = false, length = 20)
    private String kenhBan;

    @Column(name = "trangThai", nullable = false, length = 20)
    private String trangThai = "CHO_XAC_NHAN";

    @Column(name = "tongTien", nullable = false)
    private BigDecimal tongTien = BigDecimal.ZERO;

    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "ghiChu", length = 255)
    private String ghiChu;

    @OneToMany(mappedBy = "hoaDon")
    private List<ChiTietHoaDon> chiTiet = new ArrayList<>();

    @OneToMany(mappedBy = "hoaDon")
    private List<ThanhToan> thanhToan = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (ngayLap == null) ngayLap = LocalDateTime.now();
    }

    public Long getMaHD() {
        return maHD;
    }

    public void setMaHD(Long maHD) {
        this.maHD = maHD;
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

    public Long getMaKho() {
        return maKho;
    }

    public void setMaKho(Long maKho) {
        this.maKho = maKho;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public String getKenhBan() {
        return kenhBan;
    }

    public void setKenhBan(String kenhBan) {
        this.kenhBan = kenhBan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal tongTien) {
        this.tongTien = tongTien;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public List<ChiTietHoaDon> getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(List<ChiTietHoaDon> chiTiet) {
        this.chiTiet = chiTiet;
    }

    public List<ThanhToan> getThanhToan() {
        return thanhToan;
    }

    public void setThanhToan(List<ThanhToan> thanhToan) {
        this.thanhToan = thanhToan;
    }
}