package com.example.consolestore.Dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class DongGioHang implements Serializable {
    private Long maSP;
    private String tenSP;
    private String hinhAnh;
    private BigDecimal donGia;
    private int soLuong;

    public DongGioHang(Long maSP, String tenSP, String hinhAnh, BigDecimal donGia, int soLuong) {
        this.maSP = maSP; this.tenSP = tenSP; this.hinhAnh = hinhAnh;
        this.donGia = donGia; this.soLuong = soLuong;
    }
    public BigDecimal getThanhTien() { return donGia.multiply(BigDecimal.valueOf(soLuong)); }

    public Long getMaSP() {
        return maSP;
    }

    public void setMaSP(Long maSP) {
        this.maSP = maSP;
    }

    public String getTenSP() {
        return tenSP;
    }

    public void setTenSP(String tenSP) {
        this.tenSP = tenSP;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }
}