package com.example.consolestore.Repository;

import com.example.consolestore.Entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update HoaDon h set h.trangThai = :moi where h.maHD = :id and h.trangThai = :cu")
    int doiTrangThai(@Param("id") Long id, @Param("cu") String cu, @Param("moi") String moi);
}