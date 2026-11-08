package com.example.consolestore.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NhanVien")
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNV")
    private Long maNV;

    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "soDienThoai", nullable = false, unique = true, length = 10)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "chucVu", length = 100)
    private String chucVu;

    @Column(name = "ngayVaoLam", nullable = false)
    private LocalDateTime ngayVaoLam;

    @Column(name = "daNghiViec", nullable = false)
    private boolean daNghiViec = false;

    @PrePersist
    void prePersist() {
        if (ngayVaoLam == null) {
            ngayVaoLam = LocalDateTime.now();
        }
    }
    public Long getMaNV() {
        return maNV;
    }

    public void setMaNV(Long maNV) {
        this.maNV = maNV;
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

    public String getChucVu() {
        return chucVu;
    }

    public void setChucVu(String chucVu) {
        this.chucVu = chucVu;
    }

    public LocalDateTime getNgayVaoLam() {
        return ngayVaoLam;
    }

    public void setNgayVaoLam(LocalDateTime ngayVaoLam) {
        this.ngayVaoLam = ngayVaoLam;
    }

    public boolean isDaNghiViec() {
        return daNghiViec;
    }

    public void setDaNghiViec(boolean daNghiViec) {
        this.daNghiViec = daNghiViec;
    }
}
