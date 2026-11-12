package com.example.consolestore.Controller;

import com.example.consolestore.Service.GioHangService;
import com.example.consolestore.Service.NghiepVuException;
import com.example.consolestore.Service.PhienDangNhap;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gio-hang")
public class GioHangController {
    private final GioHangService gioHangService;

    public GioHangController(GioHangService gioHangService) { this.gioHangService = gioHangService; }

    @GetMapping
    public String xem(HttpSession s, Model model) {
        if (PhienDangNhap.maKhachHang(s) == null) return "redirect:/dang-nhap";
        model.addAttribute("gioHang", gioHangService.lay(s));
        return "gio-hang";
    }

    @PostMapping("/them")
    public String them(@RequestParam Long maSP,
                       @RequestParam(defaultValue = "1") int soLuong,
                       @RequestParam(required = false) String quayLai,
                       HttpSession s, RedirectAttributes ra) {
        if (PhienDangNhap.maKhachHang(s) == null) return "redirect:/dang-nhap";
        try {
            gioHangService.them(s, maSP, soLuong);
            ra.addFlashAttribute("thongBao", "Đã thêm vào giỏ hàng.");
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:" + noiBo(quayLai);
    }

    @PostMapping("/cap-nhat")
    public String capNhat(@RequestParam Long maSP, @RequestParam int soLuong,
                          HttpSession s, RedirectAttributes ra) {
        if (PhienDangNhap.maKhachHang(s) == null) return "redirect:/dang-nhap";
        try {
            gioHangService.capNhat(s, maSP, soLuong);
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return "redirect:/gio-hang";
    }

    @PostMapping("/xoa")
    public String xoa(@RequestParam Long maSP, HttpSession s) {
        if (PhienDangNhap.maKhachHang(s) == null) return "redirect:/dang-nhap";
        gioHangService.xoa(s, maSP);
        return "redirect:/gio-hang";
    }

    /** Chỉ cho quay lại đường dẫn nội bộ, tránh open redirect. */
    private String noiBo(String url) {
        boolean ok = url != null && url.startsWith("/") && !url.startsWith("//") && !url.contains("\\");
        return ok ? url : "/gio-hang";
    }
}