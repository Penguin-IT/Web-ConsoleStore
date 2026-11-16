package com.example.consolestore.Repository;

import com.example.consolestore.Entity.PhieuThuCu;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhieuThuCuRepository
        extends JpaRepository<PhieuThuCu, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM PhieuThuCu WITH (UPDLOCK, HOLDLOCK)
                    WHERE maPTC = :id
                    """,
            nativeQuery = true
    )
    Optional<PhieuThuCu> lockById(
            @Param("id") Long id
    );

    @Modifying
    @Query(
            value = """
                    UPDATE PhieuThuCu
                    SET maHD = :invoiceId
                    WHERE maPTC = :id
                      AND maKH = :customerId
                      AND maHD IS NULL
                    """,
            nativeQuery = true
    )
    int attach(
            @Param("id") Long id,
            @Param("customerId") Long customerId,
            @Param("invoiceId") Long invoiceId
    );

    @Query(
            value = """
                    SELECT maPTC
                    FROM PhieuThuCu
                    WHERE maHD = :invoiceId
                    ORDER BY maPTC
                    """,
            nativeQuery = true
    )
    List<Long> linkedIds(
            @Param("invoiceId") Long invoiceId
    );
    interface ReceiptRow {
        Long getId();

        String getCustomerName();

        String getPhone();

        String getWarehouseName();

        java.time.LocalDateTime getCreatedAt();

        java.math.BigDecimal getTotalAmount();

        Long getInvoiceId();
    }

    @Query(
            value = """
            SELECT
                p.maPTC AS id,
                k.hoTen AS customerName,
                k.soDienThoai AS phone,
                w.tenKho AS warehouseName,
                p.ngayThu AS createdAt,
                p.tongTienThu AS totalAmount,
                p.maHD AS invoiceId
            FROM PhieuThuCu p
            JOIN KhachHang k ON k.maKH = p.maKH
            JOIN Kho w ON w.maKho = p.maKho
            WHERE k.soDienThoai LIKE :keyword ESCAPE '~'
               OR k.hoTen LIKE :keyword ESCAPE '~'
               OR CONVERT(NVARCHAR(30), p.maPTC)
                  LIKE :keyword ESCAPE '~'
            ORDER BY p.ngayThu DESC, p.maPTC DESC
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM PhieuThuCu p
            JOIN KhachHang k ON k.maKH = p.maKH
            WHERE k.soDienThoai LIKE :keyword ESCAPE '~'
               OR k.hoTen LIKE :keyword ESCAPE '~'
               OR CONVERT(NVARCHAR(30), p.maPTC)
                  LIKE :keyword ESCAPE '~'
            """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<ReceiptRow> searchReceipts(
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    interface UsedProduct {
        Long getId();

        String getName();

        Boolean getMachine();
    }

    @Query(
            value = """
            SELECT
                p.maSP AS id,
                p.tenSP AS name,
                CAST(
                    CASE WHEN l.tenLoai = N'Máy console'
                         THEN 1 ELSE 0 END
                    AS BIT
                ) AS machine
            FROM SanPham p
            JOIN LoaiSanPham l ON l.maLoai = p.maLoai
            WHERE p.deleted = 0
              AND p.tinhTrang = N'CU'
              AND l.tenLoai <> N'Đĩa game'
            ORDER BY p.maSP
            """,
            nativeQuery = true
    )
    List<UsedProduct> usedProducts();

    @Query(
            value = """
            SELECT *
            FROM PhieuThuCu WITH (UPDLOCK, HOLDLOCK)
            WHERE LEFT(ghiChu, 41) = :tag
            """,
            nativeQuery = true
    )
    Optional<PhieuThuCu> lockByTag(@Param("tag") String tag);

    @Query(
            value = """
            SELECT *
            FROM PhieuThuCu
            WHERE LEFT(ghiChu, 41) = :tag
            """,
            nativeQuery = true
    )
    Optional<PhieuThuCu> findByTag(@Param("tag") String tag);

    @Modifying
    @Query(
            value = """
            INSERT INTO ChiTietPhieuThuCu (
                maPTC,
                maSP,
                soLuong,
                donGiaThu,
                tinhTrang,
                ghiChu
            )
            VALUES (
                :id,
                :productId,
                :quantity,
                :price,
                :condition,
                :note
            )
            """,
            nativeQuery = true
    )
    void insertTradeLine(
            @Param("id") Long id,
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("price") java.math.BigDecimal price,
            @Param("condition") String condition,
            @Param("note") String note
    );

    @Query(
            value = """
            SELECT tongTienThu
            FROM PhieuThuCu
            WHERE maPTC = :id
            """,
            nativeQuery = true
    )
    java.math.BigDecimal tradeTotal(@Param("id") Long id);

    interface TradeLine {
        String getName();

        Integer getQuantity();

        java.math.BigDecimal getPrice();

        java.math.BigDecimal getSubtotal();

        String getCondition();

        String getNote();
    }

    @Query(
            value = """
            SELECT
                p.tenSP AS name,
                c.soLuong AS quantity,
                c.donGiaThu AS price,
                c.soLuong * c.donGiaThu AS subtotal,
                c.tinhTrang AS condition,
                c.ghiChu AS note
            FROM ChiTietPhieuThuCu c
            JOIN SanPham p ON p.maSP = c.maSP
            WHERE c.maPTC = :id
            ORDER BY c.maCTTC
            """,
            nativeQuery = true
    )
    List<TradeLine> tradeLines(@Param("id") Long id);
}