package com.example.consolestore.Repository;

import com.example.consolestore.Entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {

    @Query("select t from ThanhToan t join fetch t.hoaDon h left join fetch h.khachHang " +
            "where t.phuongThuc = 'CHUYEN_KHOAN' and t.trangThai = 'CHUA_THANH_TOAN' " +
            "and h.kenhBan = 'ONLINE' and h.trangThai = 'CHO_XAC_NHAN' order by h.ngayLap asc")
    List<ThanhToan> choDuyet();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ThanhToan t set t.trangThai = 'DA_THANH_TOAN', t.ngayThanhToan = :luc " +
            "where t.maTT = :id and t.trangThai = 'CHUA_THANH_TOAN'")
    int danhDauDaThanhToan(@Param("id") Long id, @Param("luc") LocalDateTime luc);
    Optional<ThanhToan> findFirstByHoaDon_MaHDAndPhuongThuc(Long maHD, String phuongThuc);

    @Query("select t from ThanhToan t join fetch t.hoaDon h left join fetch h.khachHang " +
            "where t.phuongThuc = 'CHUYEN_KHOAN' and h.kenhBan = 'ONLINE' order by h.ngayLap desc")
    List<ThanhToan> tatCaQrOnline();
}