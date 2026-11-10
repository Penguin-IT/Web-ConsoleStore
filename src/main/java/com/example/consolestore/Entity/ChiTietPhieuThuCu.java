
package com.example.consolestore.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietPhieuThuCu")
public class ChiTietPhieuThuCu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maCTTC")
    private Long maCTTC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maPTC", nullable = false)
    private PhieuThuCu phieuThuCu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maSP", nullable = false)
    private SanPham sanPham;

    @Column(name = "soLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "donGiaThu", nullable = false)
    private BigDecimal donGiaThu;

    @Column(name = "tinhTrang", nullable = false, length = 100)
    private String tinhTrang;

    @Column(name = "ghiChu", length = 255)
    private String ghiChu;

    @PrePersist
    void prePersist() {
        if (tinhTrang == null) {
            tinhTrang = "CU";
        }
    }

    public Long getMaCTTC() {
        return maCTTC;
    }

    public void setMaCTTC(Long maCTTC) {
        this.maCTTC = maCTTC;
    }

    public PhieuThuCu getPhieuThuCu() {
        return phieuThuCu;
    }

    public void setPhieuThuCu(PhieuThuCu phieuThuCu) {
        this.phieuThuCu = phieuThuCu;
    }

    public SanPham getSanPham() {
        return sanPham;
    }

    public void setSanPham(SanPham sanPham) {
        this.sanPham = sanPham;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getDonGiaThu() {
        return donGiaThu;
    }

    public void setDonGiaThu(BigDecimal donGiaThu) {
        this.donGiaThu = donGiaThu;
    }

    public String getTinhTrang() {
        return tinhTrang;
    }

    public void setTinhTrang(String tinhTrang) {
        this.tinhTrang = tinhTrang;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
