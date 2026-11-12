package com.example.consolestore.Controller;

import com.example.consolestore.Repository.ThanhToanRepository;
import com.example.consolestore.Service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/thanh-toan")
public class DuyetThanhToanController {
    private final ThanhToanRepository thanhToanRepository;
    private final DuyetThanhToanService duyetService;

    public DuyetThanhToanController(ThanhToanRepository thanhToanRepository, DuyetThanhToanService duyetService) {
        this.thanhToanRepository = thanhToanRepository;
        this.duyetService = duyetService;
    }

    @GetMapping
    public String danhSach(HttpSession s, Model model) {
        if (!PhienDangNhap.laAdmin(s)) return "redirect:/dang-nhap";
        model.addAttribute("dsThanhToan", thanhToanRepository.tatCaQrOnline());
        return "admin-thanh-toan";
    }

    @PostMapping("/{maTT}/duyet")
    public String duyet(@PathVariable Long maTT, HttpSession s, RedirectAttributes ra) {
        if (!PhienDangNhap.laAdmin(s)) return "redirect:/dang-nhap";
        try {
            duyetService.duyet(maTT);
            ra.addFlashAttribute("thongBao", "Đã xác nhận nhận tiền, đơn chuyển sang Đã xác nhận.");
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        } catch (DataAccessException e) {
            ra.addFlashAttribute("loi", NghiepVuException.tuLoiDb(e));
        }
        return "redirect:/admin/thanh-toan";
    }

    @PostMapping("/{maTT}/tu-choi")
    public String tuChoi(@PathVariable Long maTT, HttpSession s, RedirectAttributes ra) {
        if (!PhienDangNhap.laAdmin(s)) return "redirect:/dang-nhap";
        try {
            duyetService.tuChoi(maTT);
            ra.addFlashAttribute("thongBao", "Đã từ chối, đơn bị hủy và hoàn kho.");
        } catch (NghiepVuException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        } catch (DataAccessException e) {
            ra.addFlashAttribute("loi", NghiepVuException.tuLoiDb(e));
        }
        return "redirect:/admin/thanh-toan";
    }
}