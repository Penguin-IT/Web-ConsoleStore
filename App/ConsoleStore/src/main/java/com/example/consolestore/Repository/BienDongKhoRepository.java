package com.example.consolestore.Repository;

import com.example.consolestore.Entity.BienDongKho;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BienDongKhoRepository
        extends JpaRepository<BienDongKho, Long> {

    interface MovementRow {
        Long getId();

        LocalDateTime getOccurredAt();

        String getType();

        Integer getQuantity();

        Integer getBeforeQuantity();

        Integer getAfterQuantity();

        Long getWarehouseId();

        String getWarehouseName();

        String getEmployeeName();

        Long getDocumentId();

        String getDocumentPrefix();
    }

    @Query(
            value = """
                SELECT
                    b.maBD AS id,
                    b.thoiGian AS occurredAt,
                    b.loaiBienDong AS type,
                    b.soLuong AS quantity,
                    b.soLuongTruoc AS beforeQuantity,
                    b.soLuongSau AS afterQuantity,
                    b.maKho AS warehouseId,
                    k.tenKho AS warehouseName,
                    n.hoTen AS employeeName,
                    COALESCE(
                        pn.maPN,
                        hd.maHD,
                        tc.maPTC
                    ) AS documentId,
                    CASE
                        WHEN b.maCTPN IS NOT NULL THEN 'PN'
                        WHEN b.maCTTC IS NOT NULL THEN 'PTC'
                        ELSE 'HD'
                    END AS documentPrefix
                FROM BienDongKho b
                JOIN Kho k ON k.maKho = b.maKho
                LEFT JOIN NhanVien n ON n.maNV = b.maNV
                LEFT JOIN ChiTietPhieuNhap pn
                    ON pn.maCTPN = b.maCTPN
                LEFT JOIN ChiTietHoaDon hd
                    ON hd.maCTHD = b.maCTHD
                LEFT JOIN ChiTietPhieuThuCu tc
                    ON tc.maCTTC = b.maCTTC
                WHERE b.maSP = :productId
                  AND (:warehouseId IS NULL OR b.maKho = :warehouseId)
                  AND (:type = '' OR b.loaiBienDong = :type)
                  AND b.thoiGian >= :fromAt
                  AND b.thoiGian < :toAt
                ORDER BY b.thoiGian DESC, b.maBD DESC
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM BienDongKho b
                WHERE b.maSP = :productId
                  AND (:warehouseId IS NULL OR b.maKho = :warehouseId)
                  AND (:type = '' OR b.loaiBienDong = :type)
                  AND b.thoiGian >= :fromAt
                  AND b.thoiGian < :toAt
                """,
            nativeQuery = true
    )
    Page<MovementRow> search(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("type") String type,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable
    );
}