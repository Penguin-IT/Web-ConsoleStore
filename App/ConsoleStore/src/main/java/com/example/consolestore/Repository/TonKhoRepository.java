package com.example.consolestore.Repository;

import com.example.consolestore.Entity.TonKho;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface TonKhoRepository extends JpaRepository<TonKho,Long> {
    interface StockTotal { Long getProductId(); Long getQuantity(); }
    @Query("select t.sanPham.maSP as productId, sum(t.soLuong) as quantity from TonKho t where t.sanPham.maSP in :ids group by t.sanPham.maSP")
    List<StockTotal> sumByProductIds(@Param("ids") Collection<Long> ids);
    interface CheckoutStock {

        Long getWarehouseId();

        Long getProductId();

        Integer getQuantity();
    }

    @org.springframework.data.jpa.repository.Query(
            value = """
                SELECT
                    maKho AS warehouseId,
                    maSP AS productId,
                    soLuong AS quantity
                FROM TonKho WITH (UPDLOCK, HOLDLOCK, ROWLOCK)
                WHERE maSP IN (:ids)
                ORDER BY maSP, maKho
                """,
            nativeQuery = true
    )
    java.util.List<CheckoutStock> lockForCheckout(
            @org.springframework.data.repository.query.Param("ids")
            java.util.Collection<Long> ids
    );
    @Query(
            value = """
                SELECT
                    maKho AS warehouseId,
                    maSP AS productId,
                    soLuong AS quantity
                FROM TonKho
                WHERE maKho = :warehouseId
                  AND maSP IN (:ids)
                """,
            nativeQuery = true
    )
    List<CheckoutStock> posStocks(
            @Param("warehouseId") Long warehouseId,
            @Param("ids") Collection<Long> ids
    );

    @Query(
            value = """
                SELECT
                    maKho AS warehouseId,
                    maSP AS productId,
                    soLuong AS quantity
                FROM TonKho WITH (UPDLOCK, HOLDLOCK, ROWLOCK)
                WHERE maKho = :warehouseId
                  AND maSP IN (:ids)
                ORDER BY maSP
                """,
            nativeQuery = true
    )
    List<CheckoutStock> lockPosStocks(
            @Param("warehouseId") Long warehouseId,
            @Param("ids") Collection<Long> ids
    );
    interface InventoryRow {
        Long getProductId();

        Long getWarehouseId();

        String getProductName();

        String getWarehouseName();

        String getCategory();

        String getCondition();

        Integer getQuantity();

        Boolean getDeleted();
    }

    @Query(
            value = """
            SELECT
                t.maSP AS productId,
                t.maKho AS warehouseId,
                p.tenSP AS productName,
                k.tenKho AS warehouseName,
                l.tenLoai AS category,
                p.tinhTrang AS condition,
                t.soLuong AS quantity,
                p.deleted AS deleted
            FROM TonKho t
            JOIN SanPham p ON p.maSP = t.maSP
            JOIN Kho k ON k.maKho = t.maKho
            JOIN LoaiSanPham l ON l.maLoai = p.maLoai
            WHERE (:warehouseId IS NULL OR t.maKho = :warehouseId)
              AND (:condition = '' OR p.tinhTrang = :condition)
              AND (
                  :availability = 'ALL'
                  OR (
                      :availability = 'IN_STOCK'
                      AND t.soLuong > 0
                  )
                  OR (
                      :availability = 'OUT_OF_STOCK'
                      AND t.soLuong = 0
                  )
              )
              AND (
                  p.tenSP LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), p.maSP)
                     LIKE :keyword ESCAPE '~'
              )
            ORDER BY p.maSP, t.maKho
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM TonKho t
            JOIN SanPham p ON p.maSP = t.maSP
            WHERE (:warehouseId IS NULL OR t.maKho = :warehouseId)
              AND (:condition = '' OR p.tinhTrang = :condition)
              AND (
                  :availability = 'ALL'
                  OR (
                      :availability = 'IN_STOCK'
                      AND t.soLuong > 0
                  )
                  OR (
                      :availability = 'OUT_OF_STOCK'
                      AND t.soLuong = 0
                  )
              )
              AND (
                  p.tenSP LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), p.maSP)
                     LIKE :keyword ESCAPE '~'
              )
            """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<InventoryRow> inventorySearch(
            @Param("warehouseId") Long warehouseId,
            @Param("condition") String condition,
            @Param("availability") String availability,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(
            value = """
            SELECT COALESCE(
                SUM(CONVERT(BIGINT, soLuong)),
                0
            )
            FROM TonKho
            WHERE maSP = :productId
              AND (:warehouseId IS NULL OR maKho = :warehouseId)
            """,
            nativeQuery = true
    )
    Long inventoryQuantity(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );
    interface LowStockSummary {

        Long getProductId();

        String getProductName();

        String getImage();

        String getCategory();

        String getProductCondition();

        Long getQuantity();
    }

    @Query(
            value = """
            SELECT
                p.maSP AS productId,
                p.tenSP AS productName,
                p.hinhAnh AS image,
                l.tenLoai AS category,
                p.tinhTrang AS productCondition,
                COALESCE(s.quantity, 0) AS quantity
            FROM SanPham p
            JOIN LoaiSanPham l ON l.maLoai = p.maLoai
            LEFT JOIN (
                SELECT
                    maSP,
                    SUM(CAST(soLuong AS BIGINT)) AS quantity
                FROM TonKho
                GROUP BY maSP
            ) s ON s.maSP = p.maSP
            WHERE p.deleted = 0
              AND COALESCE(s.quantity, 0) < :threshold
            ORDER BY
                COALESCE(s.quantity, 0),
                p.maSP
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM SanPham p
            LEFT JOIN (
                SELECT
                    maSP,
                    SUM(CAST(soLuong AS BIGINT)) AS quantity
                FROM TonKho
                GROUP BY maSP
            ) s ON s.maSP = p.maSP
            WHERE p.deleted = 0
              AND COALESCE(s.quantity, 0) < :threshold
            """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<LowStockSummary> lowStock(
            @Param("threshold") int threshold,
            org.springframework.data.domain.Pageable pageable
    );
    interface SocketStock {

        Long getProductId();

        String getProductName();

        String getProductCondition();

        Long getQuantity();
    }

    @Query(value = """
        SELECT TOP (20)
            p.maSP AS productId,
            p.tenSP AS productName,
            p.tinhTrang AS productCondition,
            COALESCE(
                SUM(CAST(t.soLuong AS BIGINT)),
                0
            ) AS quantity
        FROM SanPham p
        LEFT JOIN TonKho t ON t.maSP = p.maSP
        WHERE p.deleted = 0
          AND (
              (:productId IS NOT NULL AND p.maSP = :productId)
              OR (
                  :productId IS NULL
                  AND p.tenSP LIKE :keyword ESCAPE '~'
              )
          )
        GROUP BY
            p.maSP,
            p.tenSP,
            p.tinhTrang
        ORDER BY p.maSP
        """, nativeQuery = true)
    List<SocketStock> socketStock(
            @Param("productId") Long productId,
            @Param("keyword") String keyword
    );
}
