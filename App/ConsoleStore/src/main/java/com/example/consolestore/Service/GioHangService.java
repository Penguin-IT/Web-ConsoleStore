package com.example.consolestore.Service;

import com.example.consolestore.Dto.DongGioHang;
import com.example.consolestore.Dto.GioHang;
import com.example.consolestore.Entity.SanPham;
import com.example.consolestore.Repository.SanPhamRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class GioHangService {
    public static final String KEY = "gioHang";
    public static final long MA_KHO_ONLINE = 1L;      // Kho chính Quận 7

    private final SanPhamRepository sanPhamRepository;
    private final KhoTraCuuService kho;

    public GioHangService(SanPhamRepository sanPhamRepository, KhoTraCuuService kho) {
        this.sanPhamRepository = sanPhamRepository;
        this.kho = kho;
    }

    public GioHang lay(HttpSession s) {
        GioHang g = (GioHang) s.getAttribute(KEY);
        if (g == null) { g = new GioHang(); s.setAttribute(KEY, g); }
        return g;
    }

    public void them(HttpSession s, Long maSP, int soLuong) {
        if (soLuong < 1) throw new NghiepVuException("Số lượng phải từ 1 trở lên.");
        SanPham sp = sanPhamRepository.findById(maSP).filter(x -> !x.isDeleted())
                .orElseThrow(() -> new NghiepVuException("Sản phẩm không tồn tại hoặc đã ngừng bán."));
        GioHang g = lay(s);
        DongGioHang d = g.tim(maSP);
        int moi = (d == null ? 0 : d.getSoLuong()) + soLuong;
        kiemTraTon(sp.getTenSP(), maSP, moi);
        if (d == null) g.them(new DongGioHang(maSP, sp.getTenSP(), sp.getHinhAnh(), sp.getGiaBan(), moi));
        else d.setSoLuong(moi);
    }

    public void capNhat(HttpSession s, Long maSP, int soLuong) {
        GioHang g = lay(s);
        DongGioHang d = g.tim(maSP);
        if (d == null) return;
        if (soLuong <= 0) { g.xoa(maSP); return; }
        kiemTraTon(d.getTenSP(), maSP, soLuong);
        d.setSoLuong(soLuong);
    }

    public void xoa(HttpSession s, Long maSP) { lay(s).xoa(maSP); }
    public void xoaHet(HttpSession s) { lay(s).xoaHet(); }

    private void kiemTraTon(String ten, Long maSP, int can) {
        int ton = kho.tonKho(MA_KHO_ONLINE, maSP);
        if (can > ton) throw new NghiepVuException("\"" + ten + "\" chỉ còn " + ton + " sản phẩm trong kho.");
    }
}