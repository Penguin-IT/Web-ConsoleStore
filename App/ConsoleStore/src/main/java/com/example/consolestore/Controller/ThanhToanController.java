package com.example.consolestore.Controller;

import com.example.consolestore.Dto.GioHang;
import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Entity.KhachHang;
import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.KhachHangRepository;
import com.example.consolestore.Service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import com.example.consolestore.Service.QrService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
@Controller
@RequestMapping("/thanh-toan")
public class ThanhToanController {
    private final QrService qrService;
    private final GioHangService gioHangService;
    private final DatHangService datHangService;
    private final KhachHangRepository khachHangRepository;
    private final HoaDonRepository hoaDonRepository;

    public ThanhToanController(QrService qrService, GioHangService gioHangService, DatHangService datHangService,
                               KhachHangRepository khachHangRepository, HoaDonRepository hoaDonRepository) {
        this.qrService = qrService;
        this.gioHangService = gioHangService;
        this.datHangService = datHangService;
        this.khachHangRepository = khachHangRepository;
        this.hoaDonRepository = hoaDonRepository;
    }
    @GetMapping(value = "/qr-anh/{maHD}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] qrAnh(@PathVariable Long maHD, HttpSession s, HttpServletRequest req) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        if (maKH == null || donCuaToi(maHD, maKH) == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String goc = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
        return qrService.taoPng(goc + "/ngan-hang-gia-lap/" + maHD, 300);
    }

    /** Trang QR gọi định kỳ để biết đã nhận tiền chưa. */
    @GetMapping(value = "/trang-thai/{maHD}", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String trangThai(@PathVariable Long maHD, HttpSession s) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        HoaDon hd = maKH == null ? null : donCuaToi(maHD, maKH);
        if (hd == null) return "NOT_FOUND";
        if ("HUY".equals(hd.getTrangThai())) return "CANCELLED";
        boolean daTra = hd.getThanhToan().stream().anyMatch(t -> "DA_THANH_TOAN".equals(t.getTrangThai()));
        return daTra ? "PAID" : "PENDING";
    }
    @GetMapping
    public String form(HttpSession s, Model model, RedirectAttributes ra) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        if (maKH == null) return "redirect:/dang-nhap";           // chỉ vào được khi đã đăng nhập
        GioHang gio = gioHangService.lay(s);
        if (gio.isEmpty()) {
            ra.addFlashAttribute("loi", "Giỏ hàng đang trống.");
            return "redirect:/gio-hang";
        }
        KhachHang kh = khachHangRepository.findById(maKH).orElseThrow();
        model.addAttribute("gioHang", gio);
        model.addAttribute("hoTen", kh.getHoTen());               // tự điền, cho sửa
        model.addAttribute("soDienThoai", kh.getSoDienThoai());
        model.addAttribute("diaChi", kh.getDiaChi());
        return "thanh-toan";
    }

    @PostMapping
    public String datHang(@RequestParam(defaultValue = "") String hoTen,
                          @RequestParam(defaultValue = "") String soDienThoai,
                          @RequestParam(defaultValue = "") String diaChi,
                          HttpSession s, Model model) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        if (maKH == null) return "redirect:/dang-nhap";
        GioHang gio = gioHangService.lay(s);
        if (gio.isEmpty()) return "redirect:/gio-hang";

        hoTen = hoTen.trim(); soDienThoai = soDienThoai.trim(); diaChi = diaChi.trim();
        Map<String, String> loiTruong = new LinkedHashMap<>();
        if (hoTen.isEmpty() || hoTen.length() > 100) loiTruong.put("hoTen", "Vui lòng nhập họ tên (tối đa 100 ký tự).");
        if (!soDienThoai.matches("0\\d{9}")) loiTruong.put("soDienThoai", "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        if (diaChi.isEmpty() || diaChi.length() > 255) loiTruong.put("diaChi", "Vui lòng nhập địa chỉ giao hàng (tối đa 255 ký tự).");

        if (loiTruong.isEmpty()) {
            try {
                Long maHD = datHangService.datHangOnline(maKH, hoTen, soDienThoai, diaChi, gio);
                gioHangService.xoaHet(s);
                return "redirect:/thanh-toan/qr/" + maHD;
            } catch (NghiepVuException e) {
                model.addAttribute("loiChung", e.getMessage());
            } catch (DataAccessException e) {
                model.addAttribute("loiChung", NghiepVuException.tuLoiDb(e));
            }
        }
        model.addAttribute("loiTruong", loiTruong);
        model.addAttribute("gioHang", gio);
        model.addAttribute("hoTen", hoTen);
        model.addAttribute("soDienThoai", soDienThoai);
        model.addAttribute("diaChi", diaChi);
        return "thanh-toan";
    }

    @GetMapping("/qr/{maHD}")
    public String qr(@PathVariable Long maHD, HttpSession s, Model model) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        if (maKH == null) return "redirect:/dang-nhap";
        HoaDon hd = donCuaToi(maHD, maKH);
        if (hd == null) return "redirect:/gio-hang";
        if (!"CHO_XAC_NHAN".equals(hd.getTrangThai())) return "redirect:/thanh-toan/ket-qua/" + maHD;
        model.addAttribute("hoaDon", hd);
        return "thanh-toan-qr";
    }

    @GetMapping("/ket-qua/{maHD}")
    public String ketQua(@PathVariable Long maHD, HttpSession s, Model model) {
        Long maKH = PhienDangNhap.maKhachHang(s);
        if (maKH == null) return "redirect:/dang-nhap";
        HoaDon hd = donCuaToi(maHD, maKH);
        if (hd == null) return "redirect:/gio-hang";
        model.addAttribute("hoaDon", hd);
        return "thanh-toan-ket-qua";
    }

    /** Chỉ cho xem đơn của chính mình. */
    private HoaDon donCuaToi(Long maHD, Long maKH) {
        return hoaDonRepository.findById(maHD)
                .filter(h -> h.getKhachHang() != null && maKH.equals(h.getKhachHang().getMaKH()))
                .orElse(null);
    }
}