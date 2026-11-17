package com.example.consolestore.Repository;

import com.example.consolestore.Entity.PhieuNhap;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface PhieuNhapRepository
        extends JpaRepository<PhieuNhap, Long> {

    interface ReceiptRow {
        Long getId();

        String getSupplierName();

        String getWarehouseName();

        String getEmployeeName();

        LocalDateTime getCreatedAt();

        BigDecimal getTotalAmount();

        String getNote();
    }

    @Query(
            value = """
                SELECT
                    p.maPN AS id,
                    n.tenNCC AS supplierName,
                    k.tenKho AS warehouseName,
                    v.hoTen AS employeeName,
                    p.ngayNhap AS createdAt,
                    p.tongTien AS totalAmount,
                    p.ghiChu AS note
                FROM PhieuNhap p
                JOIN NhaCungCap n ON n.maNCC = p.maNCC
                JOIN Kho k ON k.maKho = p.maKho
                JOIN NhanVien v ON v.maNV = p.maNV
                WHERE (:supplierId IS NULL OR p.maNCC = :supplierId)
                  AND (:warehouseId IS NULL OR p.maKho = :warehouseId)
                  AND (
                      CONCAT(N'PN', p.maPN)
                          LIKE :keyword ESCAPE '~'
                      OR n.tenNCC LIKE :keyword ESCAPE '~'
                  )
                ORDER BY p.ngayNhap DESC, p.maPN DESC
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM PhieuNhap p
                JOIN NhaCungCap n ON n.maNCC = p.maNCC
                WHERE (:supplierId IS NULL OR p.maNCC = :supplierId)
                  AND (:warehouseId IS NULL OR p.maKho = :warehouseId)
                  AND (
                      CONCAT(N'PN', p.maPN)
                          LIKE :keyword ESCAPE '~'
                      OR n.tenNCC LIKE :keyword ESCAPE '~'
                  )
                """,
            nativeQuery = true
    )
    Page<ReceiptRow> search(
            @Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT
                    p.maPN AS id,
                    n.tenNCC AS supplierName,
                    k.tenKho AS warehouseName,
                    v.hoTen AS employeeName,
                    p.ngayNhap AS createdAt,
                    p.tongTien AS totalAmount,
                    p.ghiChu AS note
                FROM PhieuNhap p
                JOIN NhaCungCap n ON n.maNCC = p.maNCC
                JOIN Kho k ON k.maKho = p.maKho
                JOIN NhanVien v ON v.maNV = p.maNV
                WHERE p.maPN = :id
                """,
            nativeQuery = true
    )
    Optional<ReceiptRow> header(@Param("id") Long id);

    interface ProductOption {
        Long getId();

        String getName();

        String getCondition();

        Boolean getMachine();
    }

    @Query(
            value = """
                SELECT
                    p.maSP AS id,
                    p.tenSP AS name,
                    p.tinhTrang AS condition,
                    CAST(
                        CASE WHEN l.tenLoai = N'Máy console'
                             THEN 1 ELSE 0 END
                        AS BIT
                    ) AS machine
                FROM SanPham p
                JOIN LoaiSanPham l ON l.maLoai = p.maLoai
                WHERE p.deleted = 0
                  AND NOT (
                      l.tenLoai = N'Đĩa game'
                      AND p.tinhTrang = N'CU'
                  )
                ORDER BY p.maSP
                """,
            nativeQuery = true
    )
    List<ProductOption> productOptions();

    @Query(
            value = """
                SELECT *
                FROM PhieuNhap WITH (UPDLOCK, HOLDLOCK)
                WHERE LEFT(ghiChu, 41) = :tag
                """,
            nativeQuery = true
    )
    Optional<PhieuNhap> lockByTag(@Param("tag") String tag);

    @Query(
            value = """
                SELECT *
                FROM PhieuNhap
                WHERE LEFT(ghiChu, 41) = :tag
                """,
            nativeQuery = true
    )
    Optional<PhieuNhap> findByTag(@Param("tag") String tag);

    @Modifying
    @Query(
            value = """
                INSERT INTO ChiTietPhieuNhap (
                    maPN,
                    maSP,
                    soLuong,
                    donGiaNhap
                )
                VALUES (
                    :id,
                    :productId,
                    :quantity,
                    :price
                )
                """,
            nativeQuery = true
    )
    void insertLine(
            @Param("id") Long id,
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("price") BigDecimal price
    );

    @Query(
            value = """
                SELECT TOP (1) maCTPN
                FROM ChiTietPhieuNhap
                WHERE maPN = :id
                  AND maSP = :productId
                ORDER BY maCTPN DESC
                """,
            nativeQuery = true
    )
    Optional<Long> lastLineId(
            @Param("id") Long id,
            @Param("productId") Long productId
    );

    @Modifying
    @Query(
            value = """
                INSERT INTO ChiTietPhieuNhapSerial (
                    maCTPN,
                    maSerial
                )
                VALUES (
                    :lineId,
                    :serialId
                )
                """,
            nativeQuery = true
    )
    void linkSerial(
            @Param("lineId") Long lineId,
            @Param("serialId") Long serialId
    );

    @Query(
            value = """
                SELECT COUNT_BIG(*)
                FROM ChiTietPhieuNhapSerial
                WHERE maCTPN = :lineId
                """,
            nativeQuery = true
    )
    long serialCount(@Param("lineId") Long lineId);

    @Query(
            value = """
                SELECT tongTien
                FROM PhieuNhap
                WHERE maPN = :id
                """,
            nativeQuery = true
    )
    BigDecimal readTotal(@Param("id") Long id);

    interface LineRow {
        Long getId();

        String getName();

        String getCondition();

        Integer getQuantity();

        BigDecimal getPrice();

        BigDecimal getSubtotal();
    }

    @Query(
            value = """
                SELECT
                    c.maCTPN AS id,
                    p.tenSP AS name,
                    p.tinhTrang AS condition,
                    c.soLuong AS quantity,
                    c.donGiaNhap AS price,
                    c.soLuong * c.donGiaNhap AS subtotal
                FROM ChiTietPhieuNhap c
                JOIN SanPham p ON p.maSP = c.maSP
                WHERE c.maPN = :id
                ORDER BY c.maCTPN
                """,
            nativeQuery = true
    )
    List<LineRow> lines(@Param("id") Long id);

    interface SerialRow {
        Long getLineId();

        String getNumber();
    }

    @Query(
            value = """
                SELECT
                    c.maCTPN AS lineId,
                    s.soSerial AS number
                FROM ChiTietPhieuNhap c
                JOIN ChiTietPhieuNhapSerial x
                    ON x.maCTPN = c.maCTPN
                JOIN SerialMay s ON s.maSerial = x.maSerial
                WHERE c.maPN = :id
                ORDER BY c.maCTPN, s.maSerial
                """,
            nativeQuery = true
    )
    List<SerialRow> serialRows(@Param("id") Long id);
    interface PurchaseCostSummary {

        String getReportMonth();

        Long getSupplierId();

        String getSupplierName();

        Long getReceiptCount();

        BigDecimal getAmount();
    }

    @Query(value = """
        SELECT
            CONVERT(CHAR(7), p.ngayNhap, 23) AS reportMonth,
            p.maNCC AS supplierId,
            n.tenNCC AS supplierName,
            COUNT_BIG(*) AS receiptCount,
            SUM(p.tongTien) AS amount
        FROM PhieuNhap p
        JOIN NhaCungCap n ON n.maNCC = p.maNCC
        WHERE p.ngayNhap >= :fromDate
          AND p.ngayNhap < :toExclusive
          AND (:supplierId IS NULL OR p.maNCC = :supplierId)
        GROUP BY
            CONVERT(CHAR(7), p.ngayNhap, 23),
            p.maNCC,
            n.tenNCC
        ORDER BY reportMonth, p.maNCC
        """, nativeQuery = true)
    List<PurchaseCostSummary> purchaseCostSummary(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("supplierId") Long supplierId
    );
}