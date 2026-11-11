
package com.example.consolestore.Service;

import com.example.consolestore.DTO.PhieuThuCuDTO;
import com.example.consolestore.DTO.ChiTietPhieuThuCuDTO;

import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PhieuThuCuService {

    private final PhieuThuCuRepository phieuThuCuRepository;
    private final ChiTietPhieuThuCuRepository chiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final HoaDonRepository hoaDonRepository;
    private final KhoRepository khoRepository;
    private final SanPhamRepository sanPhamRepository;

    public PhieuThuCuService(
            PhieuThuCuRepository phieuThuCuRepository,
            ChiTietPhieuThuCuRepository chiTietRepository,
            KhachHangRepository khachHangRepository,
            NhanVienRepository nhanVienRepository,
            HoaDonRepository hoaDonRepository,
            KhoRepository khoRepository,
            SanPhamRepository sanPhamRepository) {

        this.phieuThuCuRepository = phieuThuCuRepository;
        this.chiTietRepository = chiTietRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.khoRepository = khoRepository;
        this.sanPhamRepository = sanPhamRepository;
    }

    // Lấy tất cả phiếu thu cũ
    public List<PhieuThuCu> getAll() {
        return phieuThuCuRepository.findAll();
    }

    // Tìm phiếu theo mã
    public PhieuThuCu getById(Long maPTC) {
        return phieuThuCuRepository.findById(maPTC).orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu thu cũ!"));
    }

    // Tính tổng tiền thu
    public BigDecimal tinhTongTienThu(
            List<ChiTietPhieuThuCuDTO> chiTiet) {

        BigDecimal tongTien = BigDecimal.ZERO;

        for (ChiTietPhieuThuCuDTO ct : chiTiet) {

            if (ct.getSoLuong() == null || ct.getSoLuong() <= 0) {
                throw new RuntimeException("Số lượng phải lớn hơn 0!");
            }

            if (ct.getDonGiaThu() == null || ct.getDonGiaThu().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Đơn giá thu không hợp lệ!");
            }

            BigDecimal thanhTien = ct.getDonGiaThu().multiply(BigDecimal.valueOf(ct.getSoLuong()));
            tongTien = tongTien.add(thanhTien);
        }

        return tongTien;
    }

    // Tạo phiếu thu cũ và các chi tiết
    @Transactional
    public PhieuThuCu taoPhieuThuCu(PhieuThuCuDTO dto) {

        if (dto.getChiTiet() == null || dto.getChiTiet().isEmpty()) {
            throw new RuntimeException("Phiếu thu cũ phải có ít nhất một sản phẩm!");
        }

        PhieuThuCu phieu = new PhieuThuCu();

        phieu.setKhachHang(
                khachHangRepository.findById(dto.getMaKH()).orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại!"))
        );

        phieu.setNhanVien(
                nhanVienRepository.findById(dto.getMaNV()).orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại!"))
        );

        if (dto.getMaHD() != null) {
            phieu.setHoaDon(
                    hoaDonRepository.findById(dto.getMaHD()).orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại!"))
            );
        }

        phieu.setKho(
                khoRepository.findById(dto.getMaKho()).orElseThrow(() -> new RuntimeException("Kho không tồn tại!"))
        );

        phieu.setTongTienThu(tinhTongTienThu(dto.getChiTiet()));
        phieu.setGhiChu(dto.getGhiChu());

        // Lưu phiếu chính trước
        PhieuThuCu phieuDaLuu = phieuThuCuRepository.save(phieu);

        // Lưu các sản phẩm trong phiếu
        for (ChiTietPhieuThuCuDTO ctDTO : dto.getChiTiet()) {

            ChiTietPhieuThuCu ct = new ChiTietPhieuThuCu();
            ct.setPhieuThuCu(phieuDaLuu);
            ct.setSanPham(
                    sanPhamRepository.findById(ctDTO.getMaSP()).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"))
            );

            ct.setSoLuong(ctDTO.getSoLuong());
            ct.setDonGiaThu(ctDTO.getDonGiaThu());
            ct.setTinhTrang(ctDTO.getTinhTrang());
            ct.setGhiChu(ctDTO.getGhiChu());

            chiTietRepository.save(ct);
        }
        return phieuDaLuu;
    }
}