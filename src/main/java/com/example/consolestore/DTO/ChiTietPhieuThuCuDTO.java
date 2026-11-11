
package com.example.consolestore.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ChiTietPhieuThuCuDTO {

    @NotNull(message = "Vui lòng chọn sản phẩm")
    private Long maSP;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;

    @NotNull(message = "Vui lòng nhập đơn giá thu")
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Đơn giá thu không được âm")
    private BigDecimal donGiaThu;

    @Size(max = 100, message = "Tình trạng không được quá 100 ký tự")
    private String tinhTrang = "CU";

    @Size(max = 255, message = "Ghi chú không được quá 255 ký tự")
    private String ghiChu;

    public Long getMaSP() {
        return maSP;
    }

    public void setMaSP(Long maSP) {
        this.maSP = maSP;
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
