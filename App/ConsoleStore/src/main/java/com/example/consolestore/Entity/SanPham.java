package com.example.consolestore.Entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "SanPham")
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maSP")
    private Long maSP;

    @Column(name = "tenSP", nullable = false, length = 100)
    private String tenSP;

    @Column(name = "moTa", length = 255)
    private String moTa;

    @Column(name = "giaBan", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "hinhAnh", length = 255)
    private String hinhAnh;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "maLoai", nullable = false)
    private Long maLoai;

    @Column(name = "maNenTang")
    private Long maNenTang;

    public Long getMaSP() { return maSP; }
    public void setMaSP(Long maSP) { this.maSP = maSP; }

    public String getTenSP() { return tenSP; }
    public void setTenSP(String tenSP) { this.tenSP = tenSP; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }

    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Long getMaLoai() { return maLoai; }
    public void setMaLoai(Long maLoai) { this.maLoai = maLoai; }

    public Long getMaNenTang() { return maNenTang; }
    public void setMaNenTang(Long maNenTang) { this.maNenTang = maNenTang; }
}