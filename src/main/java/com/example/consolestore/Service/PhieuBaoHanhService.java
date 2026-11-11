
package com.example.consolestore.Service;

import com.example.consolestore.DTO.PhieuBaoHanhDTO;
import com.example.consolestore.Entity.PhieuBaoHanh;
import com.example.consolestore.Entity.SerialMay;
import com.example.consolestore.Entity.NhanVien;

import com.example.consolestore.Repository.PhieuBaoHanhRepository;
import com.example.consolestore.Repository.SerialMayRepository;
import com.example.consolestore.Repository.NhanVienRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PhieuBaoHanhService {

    private final PhieuBaoHanhRepository phieuBaoHanhRepository;
    private final SerialMayRepository serialMayRepository;
    private final NhanVienRepository nhanVienRepository;

    public PhieuBaoHanhService(
            PhieuBaoHanhRepository phieuBaoHanhRepository,
            SerialMayRepository serialMayRepository,
            NhanVienRepository nhanVienRepository) {

        this.phieuBaoHanhRepository = phieuBaoHanhRepository;
        this.serialMayRepository = serialMayRepository;
        this.nhanVienRepository = nhanVienRepository;
    }

    // Lấy danh sách phiếu bảo hành
    public List<PhieuBaoHanh> getAll() {
        return phieuBaoHanhRepository.findAll();
    }

    // Tìm phiếu theo mã
    public PhieuBaoHanh getById(Long maBH) {
        return phieuBaoHanhRepository.findById(maBH).orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu bảo hành!"));
    }

    // Tiếp nhận bảo hành
    @Transactional
    public PhieuBaoHanh tiepNhanBaoHanh(PhieuBaoHanhDTO dto) {

        SerialMay serial = serialMayRepository.findById(dto.getMaSerial()).orElseThrow(() ->
                        new RuntimeException("Serial máy không tồn tại!"));

        NhanVien nhanVien = nhanVienRepository.findById(dto.getMaNV()).orElseThrow(() ->
                        new RuntimeException("Nhân viên không tồn tại!"));

        BigDecimal chiPhi = dto.getChiPhi() == null
                ? BigDecimal.ZERO : dto.getChiPhi();

        if (chiPhi.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Chi phí không được âm!");
        }

        PhieuBaoHanh phieu = new PhieuBaoHanh();

        phieu.setSerialMay(serial);
        phieu.setNhanVien(nhanVien);
        phieu.setNgayHenTra(dto.getNgayHenTra());
        phieu.setNoiDung(dto.getNoiDung());
        phieu.setMoTaLoi(dto.getMoTaLoi());
        phieu.setChiPhi(chiPhi);

        // Trạng thái ban đầu
        phieu.setTrangThai("TIEP_NHAN");

        // Không tự gán trangThaiBaoHanh vì database có trigger tính toán
        return phieuBaoHanhRepository.save(phieu);
    }

    // Cập nhật trạng thái bảo hành
    @Transactional
    public PhieuBaoHanh capNhatTrangThai(
            Long maBH, String trangThai) {

        PhieuBaoHanh phieu = getById(maBH);

        if (!trangThai.equals("TIEP_NHAN") && !trangThai.equals("DANG_XU_LY") && !trangThai.equals("HOAN_TAT")) {
            throw new RuntimeException("Trạng thái bảo hành không hợp lệ!");
        }

        phieu.setTrangThai(trangThai);
        return phieuBaoHanhRepository.save(phieu);
    }

    // Hoàn tất bảo hành
    @Transactional
    public PhieuBaoHanh hoanTatBaoHanh(
            Long maBH, BigDecimal chiPhi) {

        PhieuBaoHanh phieu = getById(maBH);

        if (phieu.getTrangThai().equals("HOAN_TAT")) {
            throw new RuntimeException("Phiếu bảo hành đã hoàn tất!");
        }

        if (chiPhi == null || chiPhi.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Chi phí không hợp lệ!");
        }

        phieu.setChiPhi(chiPhi);
        phieu.setTrangThai("HOAN_TAT");

        return phieuBaoHanhRepository.save(phieu);
    }
}
