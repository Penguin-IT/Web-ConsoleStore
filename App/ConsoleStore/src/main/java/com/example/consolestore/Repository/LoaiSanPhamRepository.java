package com.example.consolestore.Repository;

import com.example.consolestore.Entity.LoaiSanPham;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoaiSanPhamRepository
        extends JpaRepository<LoaiSanPham, Long> {

    Optional<LoaiSanPham> findByTenLoai(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from LoaiSanPham c
            where c.maLoai = :id
            """)
    Optional<LoaiSanPham> lockById(
            @Param("id") Long id
    );
}