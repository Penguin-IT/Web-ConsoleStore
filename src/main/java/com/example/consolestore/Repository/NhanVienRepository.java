package com.example.consolestore.Repository;

import com.example.consolestore.Entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NhanVienRepository extends JpaRepository<NhanVien,Long> {
    boolean existsBySoDienThoai(String soDienThoai);
}
