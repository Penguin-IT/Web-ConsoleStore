
package com.example.consolestore.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class PhieuThuCuDTO {

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long maKH;

    @NotEmpty(message = "Phiếu thu phải có ít nhất một sản phẩm")
    @Valid
    private List<ChiTietPhieuThuCuDTO> chiTiet = new ArrayList<>();

    @NotNull(message = "Vui lòng chọn nhân viên")
    private Long maNV;

    private Long maHD;

    @NotNull(message = "Vui lòng chọn kho")
    private Long maKho;

    @Size(max = 255, message = "Ghi chú không được quá 255 ký tự")
    private String ghiChu;


    public Long getMaKH() {
        return maKH;
    }

    public void setMaKH(Long maKH) {
        this.maKH = maKH;
    }

    public Long getMaNV() {
        return maNV;
    }

    public void setMaNV(Long maNV) {
        this.maNV = maNV;
    }

    public Long getMaHD() {
        return maHD;
    }

    public void setMaHD(Long maHD) {
        this.maHD = maHD;
    }

    public Long getMaKho() {
        return maKho;
    }

    public void setMaKho(Long maKho) {
        this.maKho = maKho;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public List<ChiTietPhieuThuCuDTO> getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(List<ChiTietPhieuThuCuDTO> chiTiet) {
        this.chiTiet = chiTiet;
    }
}
