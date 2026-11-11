package com.example.consolestore.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NhaCungCap")
public class NhaCungCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNCC")
    private Long maNCC;


    @Column(name = "tenNCC", nullable = false, length = 100)
    private String tenNCC;

    @Column(name = "soDienThoai", length = 10)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    void prePersist() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }

    public Long getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(Long maNCC) {
        this.maNCC = maNCC;
    }

    public String getTenNCC() {
        return tenNCC;
    }

    public void setTenNCC(String tenNCC) {
        this.tenNCC = tenNCC;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
