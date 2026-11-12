package com.example.consolestore.Service;

import com.example.consolestore.Entity.VaiTro;
import jakarta.servlet.http.HttpSession;

public final class PhienDangNhap {
    private PhienDangNhap() {}

    public static String vaiTro(HttpSession s) { return (String) s.getAttribute("vaiTro"); }

    public static Long maKhachHang(HttpSession s) {
        return VaiTro.KHACHHANG.equals(vaiTro(s)) ? (Long) s.getAttribute("maKH") : null;
    }
    public static Long maNhanVien(HttpSession s) { return (Long) s.getAttribute("maNV"); }

    public static boolean laAdmin(HttpSession s) { return VaiTro.ADMIN.equals(vaiTro(s)); }
    public static boolean laNhanVienHoacAdmin(HttpSession s) {
        return VaiTro.NHANVIEN.equals(vaiTro(s)) || laAdmin(s);
    }
}