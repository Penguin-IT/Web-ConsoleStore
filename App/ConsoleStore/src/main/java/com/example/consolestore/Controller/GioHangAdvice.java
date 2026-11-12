package com.example.consolestore.Controller;

import com.example.consolestore.Dto.GioHang;
import com.example.consolestore.Service.GioHangService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GioHangAdvice {
    @ModelAttribute("soLuongGio")
    public int soLuongGio(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        GioHang g = s == null ? null : (GioHang) s.getAttribute(GioHangService.KEY);
        return g == null ? 0 : g.getTongSoLuong();
    }
}