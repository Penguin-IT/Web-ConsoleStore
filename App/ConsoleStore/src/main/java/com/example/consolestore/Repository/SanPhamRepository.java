package com.example.consolestore.Repository;

import com.example.consolestore.Entity.SanPham;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SanPhamRepository extends JpaRepository<SanPham,Long>, JpaSpecificationExecutor<SanPham> {
    @Override
    @EntityGraph(attributePaths={"loaiSanPham","nenTang"})
    Page<SanPham> findAll(Specification<SanPham> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"loaiSanPham", "nenTang"})
    Optional<SanPham> findByMaSPAndDeletedFalse(Long maSP);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_READ
    )
    @org.springframework.data.jpa.repository.EntityGraph(
            attributePaths = {"loaiSanPham"}
    )
    @org.springframework.data.jpa.repository.Query("""
        select p
        from SanPham p
        where p.maSP in :ids
          and p.deleted = false
        order by p.maSP
        """)
    java.util.List<com.example.consolestore.Entity.SanPham>
    lockForCheckout(
            @org.springframework.data.repository.query.Param("ids")
            java.util.Collection<Long> ids
    );
    interface PosOption {

        Long getId();

        String getName();

        Integer getStock();

        Boolean getConsole();
    }

    @org.springframework.data.jpa.repository.Query(
            value = """
                SELECT
                    p.maSP AS id,
                    p.tenSP AS name,
                    COALESCE(t.soLuong, 0) AS stock,
                    CAST(
                        CASE
                            WHEN l.tenLoai = N'Máy console' THEN 1
                            ELSE 0
                        END AS BIT
                    ) AS console
                FROM SanPham p
                JOIN LoaiSanPham l ON l.maLoai = p.maLoai
                LEFT JOIN TonKho t
                    ON t.maSP = p.maSP
                   AND t.maKho = :warehouseId
                WHERE p.deleted = 0
                ORDER BY p.maSP
                """,
            nativeQuery = true
    )
    java.util.List<PosOption> posOptions(
            @org.springframework.data.repository.query.Param("warehouseId")
            Long warehouseId
    );
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "loaiSanPham",
            "nenTang"
    })
    @Query("""
        select p
        from SanPham p
        where p.maSP = :id
        """)
    Optional<SanPham> lockCatalogProduct(
            @Param("id") Long id
    );

    @EntityGraph(attributePaths = {
            "loaiSanPham",
            "nenTang"
    })
    List<SanPham> findByMaSPGoc(Long baseProductId);

    @EntityGraph(attributePaths = {
            "loaiSanPham",
            "nenTang"
    })
    List<SanPham> findByDeletedFalseAndTinhTrangOrderByMaSPAsc(
            String condition
    );

    boolean existsByLoaiSanPham_MaLoai(Long categoryId);

    boolean existsByNenTang_MaNenTang(Long platformId);

    @Query(value = """
        SELECT CASE
            WHEN EXISTS (
                SELECT 1
                FROM TonKho
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM SerialMay
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM ChiTietHoaDon
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM ChiTietPhieuNhap
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM ChiTietPhieuThuCu
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM BienDongKho
                WHERE maSP = :id
            )
            OR EXISTS (
                SELECT 1
                FROM LichSuTachMaSanPham
                WHERE maSPGoc = :id OR maSPMoi = :id
            )
            THEN 1 ELSE 0
        END
        """, nativeQuery = true)
    int hasBusinessReferences(@Param("id") Long id);
}
