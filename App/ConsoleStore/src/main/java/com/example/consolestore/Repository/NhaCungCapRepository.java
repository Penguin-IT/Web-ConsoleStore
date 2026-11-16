package com.example.consolestore.Repository;

import com.example.consolestore.Entity.NhaCungCap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NhaCungCapRepository extends JpaRepository<NhaCungCap, Long> {

    @Query(
            value = """
                SELECT n.*
                FROM NhaCungCap n
                WHERE n.tenNCC LIKE :keyword ESCAPE '~'
                   OR COALESCE(n.soDienThoai, N'')
                      LIKE :keyword ESCAPE '~'
                   OR COALESCE(n.email, N'')
                      LIKE :keyword ESCAPE '~'
                   OR CONCAT(N'NCC', n.maNCC)
                      LIKE :keyword ESCAPE '~'
                ORDER BY n.ngayTao DESC, n.maNCC DESC
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM NhaCungCap n
                WHERE n.tenNCC LIKE :keyword ESCAPE '~'
                   OR COALESCE(n.soDienThoai, N'')
                      LIKE :keyword ESCAPE '~'
                   OR COALESCE(n.email, N'')
                      LIKE :keyword ESCAPE '~'
                   OR CONCAT(N'NCC', n.maNCC)
                      LIKE :keyword ESCAPE '~'
                """,
            nativeQuery = true
    )
    Page<NhaCungCap> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT *
                FROM NhaCungCap WITH (UPDLOCK, HOLDLOCK)
                WHERE maNCC = :id
                """,
            nativeQuery = true
    )
    Optional<NhaCungCap> lockById(@Param("id") Long id);

    @Query(
            value = """
                SELECT COUNT_BIG(*)
                FROM PhieuNhap
                WHERE maNCC = :id
                """,
            nativeQuery = true
    )
    long purchaseCount(@Param("id") Long id);

    @Modifying
    @Query(
            value = """
                DELETE FROM NhaCungCap
                WHERE maNCC = :id
                  AND NOT EXISTS (
                      SELECT 1
                      FROM PhieuNhap
                      WHERE maNCC = :id
                  )
                """,
            nativeQuery = true
    )
    int deleteIfUnused(@Param("id") Long id);


}