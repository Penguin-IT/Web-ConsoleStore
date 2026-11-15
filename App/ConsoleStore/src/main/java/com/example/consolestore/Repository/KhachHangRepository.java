package com.example.consolestore.Repository;

import com.example.consolestore.Entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KhachHangRepository extends JpaRepository<KhachHang,Long> {
    boolean existsBySoDienThoai(String soDienThoai);
    List<KhachHang> findByHoTenContainingIgnoreCaseOrSoDienThoaiContaining(String ten, String sdt);
}
