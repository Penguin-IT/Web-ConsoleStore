package com.example.consolestore.Controller;

import com.example.consolestore.Service.GioHangService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DevLoginController {
    private final GioHangService gioHangService;
    public DevLoginController(GioHangService g) { this.gioHangService = g; }

    @GetMapping("/dev/login/{vaiTro}/{id}")
    @ResponseBody
    public String login(@PathVariable String vaiTro, @PathVariable Long id, HttpSession s) {
        s.setAttribute("vaiTro", vaiTro.toUpperCase());
        s.setAttribute(vaiTro.equalsIgnoreCase("KHACHHANG") ? "maKH" : "maNV", id);
        return "OK " + vaiTro + " " + id;
    }

    @GetMapping("/dev/them/{maSP}")      // thêm nhanh 1 sản phẩm vào giỏ khi chưa có trang của Nhật
    public String them(@PathVariable Long maSP, HttpSession s) {
        gioHangService.them(s, maSP, 1);
        return "redirect:/gio-hang";
    }
}