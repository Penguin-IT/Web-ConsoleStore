package com.example.consolestore.Controller;

import com.example.consolestore.Entity.ThanhToan;
import com.example.consolestore.Repository.ThanhToanRepository;
import com.example.consolestore.Service.DuyetThanhToanService;
import com.example.consolestore.Service.NghiepVuException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ngan-hang-gia-lap")
public class NganHangGiaLapController {
    private final ThanhToanRepository thanhToanRepository;
    private final DuyetThanhToanService duyetService;

    public NganHangGiaLapController(ThanhToanRepository thanhToanRepository,
                                    DuyetThanhToanService duyetService) {
        this.thanhToanRepository = thanhToanRepository;
        this.duyetService = duyetService;
    }

    /** Màn hình khi khách quét QR: hiện đúng số tiền và nội dung đã được gắn sẵn. */
    @GetMapping("/{maHD}")
    public String hienThi(@PathVariable Long maHD, Model model) {
        ThanhToan tt = thanhToanRepository
                .findFirstByHoaDon_MaHDAndPhuongThuc(maHD, "CHUYEN_KHOAN").orElse(null);
        String trangThai;
        if (tt == null) trangThai = "KHONG_TIM_THAY";
        else if ("DA_THANH_TOAN".equals(tt.getTrangThai())) trangThai = "DA_THANH_TOAN";
        else if ("HUY".equals(tt.getHoaDon().getTrangThai())) trangThai = "DA_HUY";
        else trangThai = "CHO";

        model.addAttribute("trangThai", trangThai);
        model.addAttribute("maHD", maHD);
        if (tt != null) model.addAttribute("soTien", tt.getSoTien());
        return "ngan-hang-gia-lap";
    }

    /** Khách bấm "Xác nhận chuyển tiền" = ngân hàng báo giao dịch thành công về hệ thống (webhook giả lập). */
    @PostMapping("/{maHD}/chuyen")
    public String chuyen(@PathVariable Long maHD) {
        thanhToanRepository.findFirstByHoaDon_MaHDAndPhuongThuc(maHD, "CHUYEN_KHOAN").ifPresent(tt -> {
            try {
                duyetService.duyet(tt.getMaTT());      // chuyển ThanhToan -> DA_THANH_TOAN, HoaDon -> DA_XAC_NHAN
            } catch (NghiepVuException ignored) {
                // đã thanh toán hoặc đơn đã hủy: trang bên dưới hiện đúng trạng thái
            }
        });
        return "redirect:/ngan-hang-gia-lap/" + maHD;
    }
}