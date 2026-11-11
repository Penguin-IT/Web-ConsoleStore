package com.example.consolestore.Entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="TaiKhoan")
public class TaiKhoan {
    @Id
    @Column(name = "maTK")
    private Long maTK;

    @Column(name = "tenDangNhap", nullable = false, unique = true, length = 100)
    private String tenDangNhap;

    @Column(name = "matKhauMaHoa", nullable = false, length = 255)
    private String matKhauMaHoa;

    @Column(name = "vaiTro", nullable = false, length = 20)
    private String vaiTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKH")
    private KhachHang khachHang;

    @Column(name = "biKhoa", nullable = false)
    private boolean biKhoa = false;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    void prePersist() {
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }
    public Long getMaTK() {
        return maTK;
    }

    public void setMaTK(Long maTK) {
        this.maTK = maTK;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhauMaHoa() {
        return matKhauMaHoa;
    }

    public void setMatKhauMaHoa(String matKhauMaHoa) {
        this.matKhauMaHoa = matKhauMaHoa;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public boolean isBiKhoa() {
        return biKhoa;
    }

    public void setBiKhoa(boolean biKhoa) {
        this.biKhoa = biKhoa;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
}
