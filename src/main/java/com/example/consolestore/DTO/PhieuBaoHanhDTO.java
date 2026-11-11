
package com.example.consolestore.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PhieuBaoHanhDTO {

    @NotNull(message = "Vui lòng chọn Serial máy")
    private Long maSerial;

    @NotNull(message = "Vui lòng chọn nhân viên tiếp nhận")
    private Long maNV;

    private LocalDateTime ngayHenTra;

    @Size(max = 255, message = "Nội dung không được quá 255 ký tự")
    private String noiDung;

    @Size(max = 255, message = "Mô tả lỗi không được quá 255 ký tự")
    private String moTaLoi;

    @DecimalMin(value = "0.0", inclusive = true,
            message = "Chi phí không được âm")
    private BigDecimal chiPhi = BigDecimal.ZERO;

    public Long getMaSerial() {
        return maSerial;
    }

    public void setMaSerial(Long maSerial) {
        this.maSerial = maSerial;
    }

    public Long getMaNV() {
        return maNV;
    }

    public void setMaNV(Long maNV) {
        this.maNV = maNV;
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
}
