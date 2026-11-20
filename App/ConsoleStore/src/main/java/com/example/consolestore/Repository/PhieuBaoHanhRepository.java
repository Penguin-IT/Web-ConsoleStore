package com.example.consolestore.Repository;

import com.example.consolestore.Entity.PhieuBaoHanh;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PhieuBaoHanhRepository
        extends JpaRepository<PhieuBaoHanh, Long> {

    interface Lookup {
        Long getSerialId();

        String getNumber();

        String getProductName();

        String getSerialStatus();

        Long getInvoiceId();

        String getCustomerName();

        String getPhone();

        LocalDateTime getSoldAt();

        LocalDateTime getExpiresAt();

        LocalDateTime getServerNow();

        Boolean getCovered();

        Long getActiveId();
    }

    @Query(
            value = """
                SELECT
                    s.maSerial AS serialId,
                    s.soSerial AS number,
                    p.tenSP AS productName,
                    s.trangThai AS serialStatus,
                    sale.maHD AS invoiceId,
                    sale.customerName,
                    sale.phone,
                    sale.ngayLap AS soldAt,
                    DATEADD(MONTH, 12, sale.ngayLap) AS expiresAt,
                    GETDATE() AS serverNow,
                    CAST(
                        CASE
                            WHEN sale.ngayLap <= GETDATE()
                             AND GETDATE() < DATEADD(
                                 MONTH, 12, sale.ngayLap
                             )
                            THEN 1
                            ELSE 0
                        END AS BIT
                    ) AS covered,
                    (
                        SELECT TOP (1) b.maBH
                        FROM PhieuBaoHanh b
                        WHERE b.maSerial = s.maSerial
                          AND b.trangThai <> N'HOAN_TAT'
                        ORDER BY b.maBH DESC
                    ) AS activeId
                FROM SerialMay s
                JOIN SanPham p ON p.maSP = s.maSP
                OUTER APPLY (
                    SELECT TOP (1)
                        h.maHD,
                        h.ngayLap,
                        COALESCE(
                            k.hoTen,
                            h.tenNguoiNhan
                        ) AS customerName,
                        COALESCE(
                            k.soDienThoai,
                            h.soDienThoaiNhan
                        ) AS phone
                    FROM ChiTietHoaDon c
                    JOIN HoaDon h ON h.maHD = c.maHD
                    LEFT JOIN KhachHang k ON k.maKH = h.maKH
                    WHERE c.maSerial = s.maSerial
                      AND h.trangThai <> N'HUY'
                    ORDER BY h.ngayLap, h.maHD
                ) sale
                WHERE s.soSerial = :number
                """,
            nativeQuery = true
    )
    Optional<Lookup> lookup(@Param("number") String number);

    interface Row {
        Long getId();

        String getNumber();

        String getProductName();

        LocalDateTime getReceivedAt();

        LocalDateTime getDueAt();

        String getStatus();

        Boolean getCovered();

        BigDecimal getCost();
    }

    @Query(
            value = """
                SELECT
                    b.maBH AS id,
                    s.soSerial AS number,
                    p.tenSP AS productName,
                    b.ngayTiepNhan AS receivedAt,
                    b.ngayHenTra AS dueAt,
                    b.trangThai AS status,
                    b.trangThaiBaoHanh AS covered,
                    b.chiPhi AS cost
                FROM PhieuBaoHanh b
                JOIN SerialMay s ON s.maSerial = b.maSerial
                JOIN SanPham p ON p.maSP = s.maSP
                WHERE (:status = '' OR b.trangThai = :status)
                  AND (:fromDate IS NULL OR b.ngayTiepNhan >= :fromDate)
                  AND (:toExclusive IS NULL OR b.ngayTiepNhan < :toExclusive)
                  AND (
                      s.soSerial LIKE :keyword ESCAPE '~'
                      OR p.tenSP LIKE :keyword ESCAPE '~'
                      OR (N'BH' + CONVERT(NVARCHAR(30), b.maBH)) LIKE :keyword ESCAPE '~'
                      OR CONVERT(NVARCHAR(30), b.maBH) LIKE :keyword ESCAPE '~'
                  )
                ORDER BY b.ngayTiepNhan DESC, b.maBH DESC
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM PhieuBaoHanh b
                JOIN SerialMay s ON s.maSerial = b.maSerial
                JOIN SanPham p ON p.maSP = s.maSP
                WHERE (:status = '' OR b.trangThai = :status)
                  AND (:fromDate IS NULL OR b.ngayTiepNhan >= :fromDate)
                  AND (:toExclusive IS NULL OR b.ngayTiepNhan < :toExclusive)
                  AND (
                      s.soSerial LIKE :keyword ESCAPE '~'
                      OR p.tenSP LIKE :keyword ESCAPE '~'
                      OR (N'BH' + CONVERT(NVARCHAR(30), b.maBH)) LIKE :keyword ESCAPE '~'
                      OR CONVERT(NVARCHAR(30), b.maBH) LIKE :keyword ESCAPE '~'
                  )
                """,
            nativeQuery = true
    )
    Page<Row> search(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toExclusive") java.time.LocalDateTime toExclusive,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT *
                FROM PhieuBaoHanh WITH (UPDLOCK, HOLDLOCK)
                WHERE LEFT(noiDung, 41) = :tag
                """,
            nativeQuery = true
    )
    Optional<PhieuBaoHanh> lockByTag(@Param("tag") String tag);

    @Query(
            value = """
                SELECT *
                FROM PhieuBaoHanh
                WHERE LEFT(noiDung, 41) = :tag
                """,
            nativeQuery = true
    )
    Optional<PhieuBaoHanh> findByTag(@Param("tag") String tag);

    @Query(
            value = """
                SELECT *
                FROM PhieuBaoHanh WITH (UPDLOCK, HOLDLOCK)
                WHERE maBH = :id
                """,
            nativeQuery = true
    )
    Optional<PhieuBaoHanh> lockById(@Param("id") Long id);

    @Query(
            value = """
                EXEC sp_TiepNhanBaoHanh
                    :number,
                    :employeeId,
                    :description,
                    :dueAt
                """,
            nativeQuery = true
    )
    Long receive(
            @Param("number") String number,
            @Param("employeeId") Long employeeId,
            @Param("description") String description,
            @Param("dueAt") LocalDateTime dueAt
    );

    @Modifying
    @Query(
            value = """
                UPDATE PhieuBaoHanh
                SET noiDung = :tag
                WHERE maBH = :id
                  AND maNV = :employeeId
                  AND trangThai = N'TIEP_NHAN'
                """,
            nativeQuery = true
    )
    void stamp(
            @Param("id") Long id,
            @Param("employeeId") Long employeeId,
            @Param("tag") String tag
    );

    @Modifying
    @Query(
            value = """
                UPDATE PhieuBaoHanh
                SET noiDung = :notes,
                    chiPhi = :cost,
                    ngayHenTra = :dueAt,
                    trangThai = :target
                WHERE maBH = :id
                  AND trangThai = :expected
                """,
            nativeQuery = true
    )
    void updateCase(
            @Param("id") Long id,
            @Param("expected") String expected,
            @Param("target") String target,
            @Param("notes") String notes,
            @Param("cost") BigDecimal cost,
            @Param("dueAt") LocalDateTime dueAt
    );

    @Query(
            value = """
                SELECT trangThai
                FROM PhieuBaoHanh
                WHERE maBH = :id
                """,
            nativeQuery = true
    )
    String status(@Param("id") Long id);
}