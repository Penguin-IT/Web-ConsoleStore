package com.example.consolestore.Repository;

import com.example.consolestore.Entity.ThanhToan;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ThanhToanRepository
        extends JpaRepository<ThanhToan, Long> {

    interface PendingRow {
        Long getId();

        Long getInvoiceId();

        String getCustomerName();

        String getPhone();

        LocalDateTime getCreatedAt();

        String getMethod();

        BigDecimal getAmount();
    }

    @Query(
            value = """
                SELECT
                    t.maTT AS id,
                    h.maHD AS invoiceId,
                    h.tenNguoiNhan AS customerName,
                    h.soDienThoaiNhan AS phone,
                    h.ngayLap AS createdAt,
                    t.phuongThuc AS method,
                    t.soTien AS amount
                FROM ThanhToan t
                JOIN HoaDon h ON h.maHD = t.maHD
                WHERE h.kenhBan = N'ONLINE'
                  AND h.trangThai = N'CHO_XAC_NHAN'
                  AND t.trangThai = N'CHUA_THANH_TOAN'
                  AND t.phuongThuc IN (
                      N'CHUYEN_KHOAN',
                      N'MOMO',
                      N'ZALOPAY'
                  )
                  AND (:method = '' OR t.phuongThuc = :method)
                  AND (
                      CONCAT(N'TT', t.maTT)
                          LIKE :keyword ESCAPE '~'
                      OR CONCAT(N'HD', h.maHD)
                          LIKE :keyword ESCAPE '~'
                      OR h.tenNguoiNhan
                          LIKE :keyword ESCAPE '~'
                      OR h.soDienThoaiNhan
                          LIKE :keyword ESCAPE '~'
                  )
                ORDER BY h.ngayLap, t.maTT
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM ThanhToan t
                JOIN HoaDon h ON h.maHD = t.maHD
                WHERE h.kenhBan = N'ONLINE'
                  AND h.trangThai = N'CHO_XAC_NHAN'
                  AND t.trangThai = N'CHUA_THANH_TOAN'
                  AND t.phuongThuc IN (
                      N'CHUYEN_KHOAN',
                      N'MOMO',
                      N'ZALOPAY'
                  )
                  AND (:method = '' OR t.phuongThuc = :method)
                  AND (
                      CONCAT(N'TT', t.maTT)
                          LIKE :keyword ESCAPE '~'
                      OR CONCAT(N'HD', h.maHD)
                          LIKE :keyword ESCAPE '~'
                      OR h.tenNguoiNhan
                          LIKE :keyword ESCAPE '~'
                      OR h.soDienThoaiNhan
                          LIKE :keyword ESCAPE '~'
                  )
                """,
            nativeQuery = true
    )
    Page<PendingRow> pending(
            @Param("keyword") String keyword,
            @Param("method") String method,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT maHD
                FROM ThanhToan
                WHERE maTT = :id
                """,
            nativeQuery = true
    )
    Optional<Long> invoiceId(@Param("id") Long id);

    @Query(
            value = """
                SELECT *
                FROM ThanhToan WITH (UPDLOCK, HOLDLOCK)
                WHERE maTT = :id
                """,
            nativeQuery = true
    )
    Optional<ThanhToan> lockById(@Param("id") Long id);

    @Modifying
    @Query(
            value = """
                UPDATE t
                SET trangThai = N'DA_THANH_TOAN',
                    ngayThanhToan = GETDATE()
                FROM ThanhToan t
                JOIN HoaDon h ON h.maHD = t.maHD
                WHERE t.maTT = :id
                  AND t.trangThai = N'CHUA_THANH_TOAN'
                  AND t.phuongThuc IN (
                      N'CHUYEN_KHOAN',
                      N'MOMO',
                      N'ZALOPAY'
                  )
                  AND h.kenhBan = N'ONLINE'
                  AND h.trangThai = N'CHO_XAC_NHAN'
                  AND t.soTien = h.tongTien
                  AND h.tongTien > 0
                """,
            nativeQuery = true
    )
    void approve(@Param("id") Long id);

    interface PaymentState {
        String getStatus();

        BigDecimal getAmount();

        LocalDateTime getPaidAt();
    }

    @Query(
            value = """
                SELECT
                    trangThai AS status,
                    soTien AS amount,
                    ngayThanhToan AS paidAt
                FROM ThanhToan
                WHERE maTT = :id
                """,
            nativeQuery = true
    )
    Optional<PaymentState> state(@Param("id") Long id);
}