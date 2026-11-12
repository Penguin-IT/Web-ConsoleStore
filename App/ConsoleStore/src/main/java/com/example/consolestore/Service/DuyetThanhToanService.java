package com.example.consolestore.Service;

import com.example.consolestore.Repository.HoaDonRepository;
import com.example.consolestore.Repository.ThanhToanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DuyetThanhToanService {
    private final ThanhToanRepository thanhToanRepository;
    private final HoaDonRepository hoaDonRepository;

    public DuyetThanhToanService(ThanhToanRepository thanhToanRepository, HoaDonRepository hoaDonRepository) {
        this.thanhToanRepository = thanhToanRepository;
        this.hoaDonRepository = hoaDonRepository;
    }

    /** Xác nhận đã nhận tiền: ThanhToan -> DA_THANH_TOAN, HoaDon -> DA_XAC_NHAN. */
    @Transactional
    public void duyet(Long maTT) {
        Long maHD = layMaHD(maTT);
        if (thanhToanRepository.danhDauDaThanhToan(maTT, LocalDateTime.now()) == 0)
            throw new NghiepVuException("Khoản thanh toán này đã được xử lý trước đó.");
        if (hoaDonRepository.doiTrangThai(maHD, "CHO_XAC_NHAN", "DA_XAC_NHAN") == 0)
            throw new NghiepVuException("Đơn #" + maHD + " không còn chờ xác nhận (có thể đã bị hủy). Thao tác được hoàn tác.");
    }

    /** Từ chối: hủy đơn, trigger tự hoàn kho. */
    @Transactional
    public void tuChoi(Long maTT) {
        Long maHD = layMaHD(maTT);
        if (hoaDonRepository.doiTrangThai(maHD, "CHO_XAC_NHAN", "HUY") == 0)
            throw new NghiepVuException("Đơn #" + maHD + " đã được xử lý trước đó.");
    }

    private Long layMaHD(Long maTT) {
        return thanhToanRepository.findById(maTT)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy khoản thanh toán."))
                .getHoaDon().getMaHD();
    }
}