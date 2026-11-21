package com.example.consolestore.Repository;

import com.example.consolestore.Entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {

    interface ReviewRow {
        String getReviewer();

        Integer getStars();

        String getContent();

        LocalDateTime getCreatedAt();
    }

    interface ReviewSummary {
        Double getAverage();

        Long getTotal();
    }

    @Query(
            value = """
                    SELECT TOP (50)
                        k.hoTen AS reviewer,
                        d.soSao AS stars,
                        d.noiDung AS content,
                        d.ngayTao AS createdAt
                    FROM DanhGia d
                    JOIN KhachHang k ON k.maKH = d.maKH
                    WHERE d.maSP = :productId
                    ORDER BY d.ngayTao DESC, d.maDG DESC
                    """,
            nativeQuery = true
    )
    List<ReviewRow> latest(@Param("productId") Long productId);

    @Query(
            value = """
                    SELECT AVG(CAST(soSao AS FLOAT)) AS average,
                           COUNT_BIG(*) AS total
                    FROM DanhGia
                    WHERE maSP = :productId
                    """,
            nativeQuery = true
    )
    ReviewSummary summary(@Param("productId") Long productId);

    @Query(
            value = """
                    SELECT COUNT_BIG(*)
                    FROM DanhGia
                    WHERE maSP = :productId
                      AND maKH = :customerId
                    """,
            nativeQuery = true
    )
    long countByProductAndCustomer(
            @Param("productId") Long productId,
            @Param("customerId") Long customerId
    );

    /** Số lần khách đã mua và nhận sản phẩm này (đơn đã hoàn tất). */
    @Query(
            value = """
                    SELECT COUNT_BIG(*)
                    FROM HoaDon h
                    JOIN ChiTietHoaDon c ON c.maHD = h.maHD
                    WHERE h.maKH = :customerId
                      AND c.maSP = :productId
                      AND h.trangThai = N'HOAN_TAT'
                    """,
            nativeQuery = true
    )
    long countCompletedPurchases(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );

    @Modifying
    @Query(
            value = """
                    INSERT INTO DanhGia (maSP, maKH, soSao, noiDung)
                    VALUES (:productId, :customerId, :stars, :content)
                    """,
            nativeQuery = true
    )
    void insert(
            @Param("productId") Long productId,
            @Param("customerId") Long customerId,
            @Param("stars") int stars,
            @Param("content") String content
    );
}