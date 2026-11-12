package com.example.consolestore.Service;

import com.example.consolestore.Dto.DongGioHang;
import com.example.consolestore.Dto.GioHang;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DatHangService {
    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final KhoTraCuuService kho;

    @PersistenceContext
    private EntityManager em;

    public DatHangService(HoaDonRepository hoaDonRepository,
                          ChiTietHoaDonRepository chiTietHoaDonRepository,
                          ThanhToanRepository thanhToanRepository,
                          SanPhamRepository sanPhamRepository,
                          KhachHangRepository khachHangRepository,
                          KhoTraCuuService kho) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.khachHangRepository = khachHangRepository;
        this.kho = kho;
    }

    /** Tạo HoaDon ONLINE (CHO_XAC_NHAN) + ChiTietHoaDon + ThanhToan (CHUA_THANH_TOAN) trong 1 transaction.
     *  Lỗi bất kỳ => rollback toàn bộ, không để lại đơn dở dang. */
    @Transactional
    public Long datHangOnline(Long maKH, String hoTen, String sdt, String diaChi, GioHang gio) {
        if (gio.isEmpty()) throw new NghiepVuException("Giỏ hàng đang trống.");

        // 1. Kiểm tra tồn, gom hết sản phẩm thiếu để báo một lần
        List<String> thieu = new ArrayList<>();
        for (DongGioHang d : gio.getDong()) {
            int ton = kho.tonKho(GioHangService.MA_KHO_ONLINE, d.getMaSP());
            if (d.getSoLuong() > ton) thieu.add(d.getTenSP() + " (còn " + ton + ")");
        }
        if (!thieu.isEmpty()) throw new NghiepVuException("Không đủ hàng: " + String.join(", ", thieu));

        // 2. Hóa đơn
        HoaDon hd = new HoaDon();
        hd.setKhachHang(khachHangRepository.getReferenceById(maKH));
        hd.setMaKho(GioHangService.MA_KHO_ONLINE);
        hd.setKenhBan("ONLINE");
        hd.setTrangThai("CHO_XAC_NHAN");
        hd.setDiaChi(diaChi);
        hd.setGhiChu("Người nhận: " + hoTen + " - SĐT: " + sdt);
        hoaDonRepository.save(hd);                       // IDENTITY => INSERT chạy ngay

        // 3. Chi tiết. Trigger tự trừ kho và cộng tongTien.
        //    Máy console: mỗi máy 1 dòng (soLuong = 1) để CN10 gán serial từng máy.
        for (DongGioHang d : gio.getDong()) {
            SanPham sp = sanPhamRepository.findById(d.getMaSP()).filter(x -> !x.isDeleted())
                    .orElseThrow(() -> new NghiepVuException("Sản phẩm \"" + d.getTenSP() + "\" đã ngừng bán."));
            boolean may = kho.laMay(d.getMaSP());
            int soDong = may ? d.getSoLuong() : 1;
            int slMoiDong = may ? 1 : d.getSoLuong();
            for (int i = 0; i < soDong; i++) {
                ChiTietHoaDon ct = new ChiTietHoaDon();
                ct.setHoaDon(hd);
                ct.setSanPham(sp);
                ct.setSoLuong(slMoiDong);
                ct.setDonGia(sp.getGiaBan());            // luôn lấy giá hiện tại trong DB
                chiTietHoaDonRepository.save(ct);
            }
        }

        // 4. tongTien do trigger cộng trong DB, nạp lại rồi mới tạo ThanhToan
        em.flush();
        em.refresh(hd);

        ThanhToan tt = new ThanhToan();
        tt.setHoaDon(hd);
        tt.setPhuongThuc("CHUYEN_KHOAN");
        tt.setSoTien(hd.getTongTien());
        tt.setTrangThai("CHUA_THANH_TOAN");
        thanhToanRepository.save(tt);

        return hd.getMaHD();
    }
}