package com.example.consolestore.Repository;

import com.example.consolestore.Entity.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang,Long> {
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone,Long id);

    @Query(
            value = """
                    SELECT k.*
                    FROM KhachHang k WITH (UPDLOCK, HOLDLOCK)
                    WHERE k.maKH = :id
                    """,
            nativeQuery = true
    )
    Optional<KhachHang> lockById(@Param("id") Long id);
    Optional<KhachHang> findByPhone(String phone);

    @Query(
            value = """
                SELECT *
                FROM KhachHang WITH (UPDLOCK, HOLDLOCK)
                WHERE soDienThoai = :phone
                """,
            nativeQuery = true
    )
    Optional<KhachHang> lockByPhone(@Param("phone") String phone);
    @Query(
            value = """
            SELECT k.*
            FROM KhachHang k
            WHERE k.hoTen LIKE :keyword ESCAPE '~'
               OR k.soDienThoai LIKE :keyword ESCAPE '~'
               OR COALESCE(k.email, N'') LIKE :keyword ESCAPE '~'
               OR CONVERT(NVARCHAR(30), k.maKH)
                  LIKE :keyword ESCAPE '~'
            ORDER BY k.ngayTao DESC, k.maKH DESC
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM KhachHang k
            WHERE k.hoTen LIKE :keyword ESCAPE '~'
               OR k.soDienThoai LIKE :keyword ESCAPE '~'
               OR COALESCE(k.email, N'') LIKE :keyword ESCAPE '~'
               OR CONVERT(NVARCHAR(30), k.maKH)
                  LIKE :keyword ESCAPE '~'
            """,
            nativeQuery = true
    )
    Page<KhachHang> searchCustomers(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    interface CustomerLinks {
        Long getInvoiceCount();
        Long getTradeInCount();
        Long getAccountCount();
    }

    @Query(
            value = """
            SELECT
                (
                    SELECT COUNT_BIG(*)
                    FROM HoaDon
                    WHERE maKH = :id
                ) AS invoiceCount,
                (
                    SELECT COUNT_BIG(*)
                    FROM PhieuThuCu
                    WHERE maKH = :id
                ) AS tradeInCount,
                (
                    SELECT COUNT_BIG(*)
                    FROM TaiKhoan
                    WHERE maKH = :id
                ) AS accountCount
            """,
            nativeQuery = true
    )
    CustomerLinks customerLinks(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query(
            value = """
            DELETE FROM KhachHang
            WHERE maKH = :id
              AND NOT EXISTS (
                  SELECT 1 FROM HoaDon WHERE maKH = :id
              )
              AND NOT EXISTS (
                  SELECT 1 FROM PhieuThuCu WHERE maKH = :id
              )
              AND NOT EXISTS (
                  SELECT 1 FROM TaiKhoan WHERE maKH = :id
              )
            """,
            nativeQuery = true
    )
    int deleteIfUnused(@Param("id") Long id);
}
