package com.example.consolestore.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="KhachHang")
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maKH")
    private Long maKH;

    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "soDienThoai", nullable = false, unique = true, length = 10)
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
    public Long getMaKH() {
        return maKH;
    }

    public void setMaKH(Long maKH) {
        this.maKH = maKH;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
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
