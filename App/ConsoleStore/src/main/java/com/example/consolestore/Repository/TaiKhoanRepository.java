package com.example.consolestore.Repository;

import com.example.consolestore.Entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan,Long> {
    TaiKhoan findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    TaiKhoan findByKhachHang_MaKH(Long maKH);

    TaiKhoan findByNhanVien_MaNV(Long maNV);
}
