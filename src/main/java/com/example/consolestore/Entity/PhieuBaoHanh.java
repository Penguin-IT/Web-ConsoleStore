
package com.example.consolestore.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuBaoHanh")
public class PhieuBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maBH")
    private Long maBH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maSerial", nullable = false)
    private SerialMay serialMay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "ngayTiepNhan", nullable = false)
    private LocalDateTime ngayTiepNhan;

    @Column(name = "trangThaiBaoHanh", nullable = false, columnDefinition = "BIT(1)")
    private Boolean trangThaiBaoHanh;

    @Column(name = "ngayHenTra")
    private LocalDateTime ngayHenTra;

    @Column(name = "noiDung", length = 255)
    private String noiDung;

    @Column(name = "moTaLoi", length = 255)
    private String moTaLoi;

    @Column(name = "chiPhi", nullable = false)
    private BigDecimal chiPhi;

    @Column(name = "trangThai", nullable = false, length = 20)
    private String trangThai;

    @PrePersist
    void prePersist() {
        if (ngayTiepNhan == null) {
            ngayTiepNhan = LocalDateTime.now();
        }
        if (trangThaiBaoHanh == null) {
            trangThaiBaoHanh = false;
        }
        if (chiPhi == null) {
            chiPhi = BigDecimal.ZERO;
        }
        if (trangThai == null) {
            trangThai = "TIEP_NHAN";
        }
    }

    public Long getMaBH() {
        return maBH;
    }

    public void setMaBH(Long maBH) {
        this.maBH = maBH;
    }

    public SerialMay getSerialMay() {
        return serialMay;
    }

    public void setSerialMay(SerialMay serialMay) {
        this.serialMay = serialMay;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public LocalDateTime getNgayTiepNhan() {
        return ngayTiepNhan;
    }

    public void setNgayTiepNhan(LocalDateTime ngayTiepNhan) {
        this.ngayTiepNhan = ngayTiepNhan;
    }

    public Boolean getTrangThaiBaoHanh() {
        return trangThaiBaoHanh;
    }

    public void setTrangThaiBaoHanh(Boolean trangThaiBaoHanh) {
        this.trangThaiBaoHanh = trangThaiBaoHanh;
    }

    public LocalDateTime getNgayHenTra() {
        return ngayHenTra;
    }

    public void setNgayHenTra(LocalDateTime ngayHenTra) {
        this.ngayHenTra = ngayHenTra;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getMoTaLoi() {
        return moTaLoi;
    }

    public void setMoTaLoi(String moTaLoi) {
        this.moTaLoi = moTaLoi;
    }

    public BigDecimal getChiPhi() {
        return chiPhi;
    }

    public void setChiPhi(BigDecimal chiPhi) {
        this.chiPhi = chiPhi;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
