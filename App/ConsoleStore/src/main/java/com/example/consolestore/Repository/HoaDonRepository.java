package com.example.consolestore.Repository;

import com.example.consolestore.Entity.HoaDon;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface HoaDonRepository
        extends JpaRepository<HoaDon, Long> {

    Optional<HoaDon> findByRequestIdAndCustomerId(
            String requestId,
            Long customerId
    );

    Optional<HoaDon> findByIdAndCustomerId(
            Long id,
            Long customerId
    );

    @Query(
            value = """
                    SELECT TOP (1) h.*
                    FROM HoaDon h WITH (UPDLOCK, HOLDLOCK)
                    WHERE h.maYeuCau = :requestId
                    """,
            nativeQuery = true
    )
    Optional<HoaDon> lockByRequestId(
            @Param("requestId") String requestId
    );

    @Query("""
            select h.totalAmount
            from HoaDon h
            where h.id = :id
            """)
    BigDecimal readTotal(@Param("id") Long id);
    @Query("""
    select sum(h.totalAmount
               - coalesce(h.shippingFee, 0)
               + coalesce(h.discount, 0))
    from HoaDon h
    where h.customerId = :customerId
      and h.status = 'HOAN_TAT'
    """)
    BigDecimal completedSpend(@Param("customerId") Long customerId);
    @Modifying
    @Query(
            value = """
                    INSERT INTO ChiTietHoaDon (
                        maHD, maSP, maKho, maSerial, soLuong, donGia
                    )
                    VALUES (
                        :invoiceId, :productId, :warehouseId,
                        NULL, :quantity, :price
                    )
                    """,
            nativeQuery = true
    )
    void insertLine(
            @Param("invoiceId") Long invoiceId,
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("quantity") int quantity,
            @Param("price") BigDecimal price
    );

    @Query("select count(h) from HoaDon h where h.couponCode = :code and h.status <> 'HUY'")
    long couponUses(@Param("code") String code);

    @Query("""
        select count(h) from HoaDon h
        where h.couponCode = :code and h.customerId = :customerId and h.status <> 'HUY'
        """)
    long couponUsesByCustomer(@Param("code") String code, @Param("customerId") Long customerId);

    @Modifying
    @Query(value = """
        UPDATE HoaDon
        SET tongTien     = tongTien + :shipping - :discount,
            phiVanChuyen = :shipping,
            giamGia      = :discount,
            maGiamGia    = NULLIF(:code, '')
        WHERE maHD = :id
        """, nativeQuery = true)
    void applyAdjustments(@Param("id") Long id,
                          @Param("shipping") BigDecimal shipping,
                          @Param("discount") BigDecimal discount,
                          @Param("code") String code);

    @Modifying
    @Query(
            value = """
                    INSERT INTO ThanhToan (
                        maHD, phuongThuc, soTien,
                        trangThai, ngayThanhToan
                    )
                    VALUES (
                        :id, :method, :amount,
                        N'CHUA_THANH_TOAN', NULL
                    )
                    """,
            nativeQuery = true
    )
    void insertPayment(
            @Param("id") Long id,
            @Param("method") String method,
            @Param("amount") BigDecimal amount
    );

    interface PaymentInfo {
        String getMethod();

        String getStatus();

        BigDecimal getAmount();
    }

    @Query(
            value = """
                    SELECT TOP (1)
                        phuongThuc AS method,
                        trangThai AS status,
                        soTien AS amount
                    FROM ThanhToan
                    WHERE maHD = :id
                    ORDER BY maTT DESC
                    """,
            nativeQuery = true
    )
    Optional<PaymentInfo> payment(@Param("id") Long id);

    interface LineInfo {
        Long getProductId();

        String getName();

        Long getQuantity();

        BigDecimal getPrice();

        BigDecimal getSubtotal();
    }

    @Query(
            value = """
                    SELECT
                        c.maSP AS productId,
                        s.tenSP AS name,
                        SUM(CONVERT(BIGINT, c.soLuong)) AS quantity,
                        c.donGia AS price,
                        SUM(
                            CONVERT(DECIMAL(18,0), c.soLuong) * c.donGia
                        ) AS subtotal
                    FROM ChiTietHoaDon c
                    JOIN SanPham s ON s.maSP = c.maSP
                    WHERE c.maHD = :id
                    GROUP BY c.maSP, s.tenSP, c.donGia
                    ORDER BY c.maSP
                    """,
            nativeQuery = true
    )
    List<LineInfo> lines(@Param("id") Long id);
    interface CustomerOrderRow {

        Long getId();

        java.time.LocalDateTime getCreatedAt();

        String getChannel();

        String getStatus();

        BigDecimal getTotalAmount();

        String getPaymentStatus();
        String getPaymentMethod();
    }

    // ── Chi tiết đơn hàng khách hàng ──────────────────────────────────────────

    interface CustomerOrderHeader {
        Long getId();
        java.time.LocalDateTime getCreatedAt();
        String getChannel();
        String getStatus();
        BigDecimal getTotalAmount();
        String getPaymentStatus();
        String getPaymentMethod();
        String getRecipientName();
        String getRecipientPhone();
        String getAddress();
        String getNote();
    }

    @Query(
            value = """
                SELECT
                    h.maHD            AS id,
                    h.ngayLap         AS createdAt,
                    h.kenhBan         AS channel,
                    h.trangThai       AS status,
                    h.tongTien        AS totalAmount,
                    t.trangThai       AS paymentStatus,
                    t.phuongThuc      AS paymentMethod,
                    h.tenNguoiNhan    AS recipientName,
                    h.soDienThoaiNhan AS recipientPhone,
                    h.diaChi          AS address,
                    h.ghiChu          AS note
                FROM HoaDon h
                LEFT JOIN ThanhToan t ON t.maHD = h.maHD
                WHERE h.maHD = :id
                  AND h.maKH = :customerId
                """,
            nativeQuery = true
    )
    Optional<CustomerOrderHeader> findCustomerOrderHeader(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

    interface CustomerOrderLine {
        String getName();
        Integer getQuantity();
        BigDecimal getPrice();
        BigDecimal getSubtotal();
        String getSerialNumber();
    }

    @Query(
            value = """
                SELECT
                    p.tenSP              AS name,
                    c.soLuong            AS quantity,
                    c.donGia             AS price,
                    c.soLuong * c.donGia AS subtotal,
                    s.soSerial           AS serialNumber
                FROM ChiTietHoaDon c
                JOIN HoaDon h  ON h.maHD  = c.maHD
                JOIN SanPham p ON p.maSP  = c.maSP
                LEFT JOIN SerialMay s ON s.maSerial = c.maSerial
                WHERE c.maHD = :id
                  AND h.maKH = :customerId
                ORDER BY c.maCTHD
                """,
            nativeQuery = true
    )
    List<CustomerOrderLine> findCustomerOrderLines(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

    @Query(
            value = """
                SELECT
                    h.maHD AS id,
                    h.ngayLap AS createdAt,
                    h.kenhBan AS channel,
                    h.trangThai AS status,
                    h.tongTien AS totalAmount,
                    t.trangThai AS paymentStatus,
                    t.phuongThuc AS paymentMethod
                FROM HoaDon h
                LEFT JOIN ThanhToan t ON t.maHD = h.maHD
                WHERE h.maKH = :customerId
                ORDER BY h.ngayLap DESC, h.maHD DESC
                """,
            countQuery = """
                SELECT COUNT_BIG(*)
                FROM HoaDon
                WHERE maKH = :customerId
                """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<CustomerOrderRow>
    findCustomerOrders(
            @Param("customerId") Long customerId,
            org.springframework.data.domain.Pageable pageable
    );

    interface OrderState {

        String getChannel();

        String getStatus();
    }

    @Query(
            value = """
                SELECT
                    kenhBan AS channel,
                    trangThai AS status
                FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
                WHERE maHD = :id
                  AND maKH = :customerId
                """,
            nativeQuery = true
    )
    Optional<OrderState> lockCustomerOrder(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );

    @Query(
            value = """
                SELECT
                    phuongThuc AS method,
                    trangThai AS status,
                    soTien AS amount
                FROM ThanhToan WITH (UPDLOCK, HOLDLOCK)
                WHERE maHD = :id
                """,
            nativeQuery = true
    )
    Optional<PaymentInfo> lockOrderPayment(
            @Param("id") Long id
    );

    @Modifying
    @Query(
            value = """
                UPDATE HoaDon
                SET trangThai = N'HUY'
                WHERE maHD = :id
                  AND maKH = :customerId
                  AND kenhBan = N'ONLINE'
                  AND trangThai = N'CHO_XAC_NHAN'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ThanhToan t
                      WHERE t.maHD = :id
                        AND t.trangThai = N'DA_THANH_TOAN'
                  )
                """,
            nativeQuery = true
    )
    int cancelUnpaidOrder(
            @Param("id") Long id,
            @Param("customerId") Long customerId
    );
    Optional<HoaDon> findByRequestId(String requestId);

    @Modifying
    @Query(
            value = """
                INSERT INTO ChiTietHoaDon (
                    maHD, maSP, maKho, maSerial, soLuong, donGia
                )
                VALUES (
                    :invoiceId, :productId, :warehouseId,
                    :serialId, :quantity, :price
                )
                """,
            nativeQuery = true
    )
    void insertPosLine(
            @Param("invoiceId") Long invoiceId,
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("serialId") Long serialId,
            @Param("quantity") int quantity,
            @Param("price") BigDecimal price
    );

    @Modifying
    @Query(
            value = """
                INSERT INTO ThanhToan (
                    maHD, phuongThuc, soTien,
                    trangThai, ngayThanhToan
                )
                VALUES (
                    :id, :method, :amount,
                    N'DA_THANH_TOAN', GETDATE()
                )
                """,
            nativeQuery = true
    )
    void insertPosPayment(
            @Param("id") Long id,
            @Param("method") String method,
            @Param("amount") BigDecimal amount
    );

    @Modifying
    @Query(
            value = """
                UPDATE HoaDon
                SET trangThai = N'HOAN_TAT'
                WHERE maHD = :id
                  AND kenhBan = N'QUAY'
                  AND trangThai = N'CHO_XAC_NHAN'
                """,
            nativeQuery = true
    )
    int finishPos(@Param("id") Long id);

    interface PosInvoiceLine {

        String getName();

        String getSerialNumber();

        Integer getQuantity();

        BigDecimal getPrice();

        BigDecimal getSubtotal();
    }

    @Query(
            value = """
                SELECT
                    p.tenSP AS name,
                    s.soSerial AS serialNumber,
                    c.soLuong AS quantity,
                    c.donGia AS price,
                    c.soLuong * c.donGia AS subtotal
                FROM ChiTietHoaDon c
                JOIN SanPham p ON p.maSP = c.maSP
                LEFT JOIN SerialMay s ON s.maSerial = c.maSerial
                WHERE c.maHD = :id
                ORDER BY c.maCTHD
                """,
            nativeQuery = true
    )
    List<PosInvoiceLine> posLines(@Param("id") Long id);
    interface OnlineRow {
        Long getId();

        java.time.LocalDateTime getCreatedAt();

        String getRecipientName();

        String getRecipientPhone();

        String getStatus();

        BigDecimal getTotalAmount();

        String getPaymentStatus();
    }

    @Query(
            value = """
            SELECT
                h.maHD AS id,
                h.ngayLap AS createdAt,
                h.tenNguoiNhan AS recipientName,
                h.soDienThoaiNhan AS recipientPhone,
                h.trangThai AS status,
                h.tongTien AS totalAmount,
                t.trangThai AS paymentStatus
            FROM HoaDon h
            LEFT JOIN ThanhToan t ON t.maHD = h.maHD
            WHERE h.kenhBan = N'ONLINE'
              AND (:status = '' OR h.trangThai = :status)
              AND (
                  h.tenNguoiNhan LIKE :keyword ESCAPE '~'
                  OR h.soDienThoaiNhan LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), h.maHD)
                     LIKE :keyword ESCAPE '~'
              )
            ORDER BY h.ngayLap DESC, h.maHD DESC
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM HoaDon h
            WHERE h.kenhBan = N'ONLINE'
              AND (:status = '' OR h.trangThai = :status)
              AND (
                  h.tenNguoiNhan LIKE :keyword ESCAPE '~'
                  OR h.soDienThoaiNhan LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), h.maHD)
                     LIKE :keyword ESCAPE '~'
              )
            """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<OnlineRow> onlineOrders(
            @Param("status") String status,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(
            value = """
            SELECT *
            FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
            WHERE maHD = :id
              AND kenhBan = N'ONLINE'
            """,
            nativeQuery = true
    )
    Optional<HoaDon> lockOnlineOrder(@Param("id") Long id);

    interface OnlineLine {
        Long getId();

        Long getProductId();

        Long getWarehouseId();

        String getWarehouseName();

        String getName();

        String getCondition();

        Boolean getMachine();

        Integer getQuantity();

        BigDecimal getPrice();

        BigDecimal getSubtotal();

        Long getSerialId();

        String getSerialNumber();
    }

    @Query(
            value = """
            SELECT
                c.maCTHD AS id,
                c.maSP AS productId,
                COALESCE(c.maKho, h.maKho) AS warehouseId,
                k.tenKho AS warehouseName,
                p.tenSP AS name,
                p.tinhTrang AS condition,
                CAST(
                    CASE WHEN l.tenLoai = N'Máy console'
                         THEN 1 ELSE 0 END
                    AS BIT
                ) AS machine,
                c.soLuong AS quantity,
                c.donGia AS price,
                c.soLuong * c.donGia AS subtotal,
                c.maSerial AS serialId,
                s.soSerial AS serialNumber
            FROM ChiTietHoaDon c
            JOIN HoaDon h ON h.maHD = c.maHD
            JOIN SanPham p ON p.maSP = c.maSP
            JOIN LoaiSanPham l ON l.maLoai = p.maLoai
            LEFT JOIN Kho k
                ON k.maKho = COALESCE(c.maKho, h.maKho)
            LEFT JOIN SerialMay s ON s.maSerial = c.maSerial
            WHERE c.maHD = :id
            ORDER BY c.maCTHD
            """,
            nativeQuery = true
    )
    List<OnlineLine> onlineLines(@Param("id") Long id);

    @Modifying
    @Query(
            value = """
            UPDATE HoaDon
            SET trangThai = :target,
                maNV = :employeeId
            WHERE maHD = :id
              AND kenhBan = N'ONLINE'
              AND trangThai = :expected
            """,
            nativeQuery = true
    )
    void changeOnlineStatus(
            @Param("id") Long id,
            @Param("expected") String expected,
            @Param("target") String target,
            @Param("employeeId") Long employeeId
    );

    @Query(
            value = """
            SELECT trangThai
            FROM HoaDon
            WHERE maHD = :id
              AND kenhBan = N'ONLINE'
            """,
            nativeQuery = true
    )
    String onlineStatus(@Param("id") Long id);

    @Modifying
    @Query(
            value = """
            UPDATE ChiTietHoaDon
            SET maSerial = :serialId
            WHERE maCTHD = :lineId
              AND maHD = :invoiceId
              AND maSerial IS NULL
            """,
            nativeQuery = true
    )
    void assignOnlineSerial(
            @Param("invoiceId") Long invoiceId,
            @Param("lineId") Long lineId,
            @Param("serialId") Long serialId
    );
    @Modifying
    @Query(
            value = """
            UPDATE HoaDon
            SET trangThai = :target,
                maNV = COALESCE(:employeeId, maNV)
            WHERE maHD = :id
              AND kenhBan = N'ONLINE'
              AND trangThai = N'CHO_XAC_NHAN'
            """,
            nativeQuery = true
    )
    void reviewOnlinePayment(
            @Param("id") Long id,
            @Param("target") String target,
            @Param("employeeId") Long employeeId
    );
    interface RevenueSummary {

        String getReportDay();

        String getChannel();

        Long getInvoiceCount();

        BigDecimal getRevenue();
    }

    @Query(value = """
        SELECT
            CONVERT(CHAR(10), h.ngayLap, 23) AS reportDay,
            h.kenhBan AS channel,
            COUNT_BIG(*) AS invoiceCount,
            SUM(h.tongTien) AS revenue
        FROM HoaDon h
        WHERE h.ngayLap >= :fromDate
          AND h.ngayLap < :toExclusive
          AND h.trangThai = N'HOAN_TAT'
          AND h.kenhBan IN (N'QUAY', N'ONLINE')
          AND EXISTS (
              SELECT 1
              FROM ThanhToan t
              WHERE t.maHD = h.maHD
                AND t.trangThai = N'DA_THANH_TOAN'
              GROUP BY t.maHD
              HAVING SUM(t.soTien) >= h.tongTien
          )
        GROUP BY
            CONVERT(CHAR(10), h.ngayLap, 23),
            h.kenhBan
        ORDER BY
            reportDay,
            channel
        """, nativeQuery = true)
    List<RevenueSummary> revenueSummary(
            @Param("fromDate")
            java.time.LocalDateTime fromDate,

            @Param("toExclusive")
            java.time.LocalDateTime toExclusive
    );
    interface BestSellerSummary {

        Long getProductId();

        String getProductName();

        String getImage();

        String getProductCondition();

        Boolean getDeleted();

        Long getSoldQuantity();

        BigDecimal getRevenue();
    }

    @Query(value = """
        SELECT TOP (:limit)
            p.maSP AS productId,
            p.tenSP AS productName,
            p.hinhAnh AS image,
            p.tinhTrang AS productCondition,
            p.deleted AS deleted,
            SUM(CAST(d.soLuong AS BIGINT)) AS soldQuantity,
            SUM(
                CAST(d.soLuong AS DECIMAL(18,0)) * d.donGia
            ) AS revenue
        FROM ChiTietHoaDon d
        JOIN HoaDon h ON h.maHD = d.maHD
        JOIN SanPham p ON p.maSP = d.maSP
        WHERE h.ngayLap >= :fromDate
          AND h.ngayLap < :toExclusive
          AND h.trangThai = N'HOAN_TAT'
          AND h.kenhBan IN (N'QUAY', N'ONLINE')
          AND EXISTS (
              SELECT 1
              FROM ThanhToan t
              WHERE t.maHD = h.maHD
                AND t.trangThai = N'DA_THANH_TOAN'
              GROUP BY t.maHD
              HAVING SUM(t.soTien) >= h.tongTien
          )
        GROUP BY
            p.maSP,
            p.tenSP,
            p.hinhAnh,
            p.tinhTrang,
            p.deleted
        ORDER BY
            soldQuantity DESC,
            revenue DESC,
            p.maSP
        """, nativeQuery = true)
    List<BestSellerSummary> bestSellers(
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toExclusive") java.time.LocalDateTime toExclusive,
            @Param("limit") int limit
    );
    @org.springframework.transaction.annotation.Transactional(
            readOnly = true,
            timeout = 15
    )
    @Query(value = """
        SELECT TOP (:batchSize) h.maHD
        FROM HoaDon h
        WHERE h.kenhBan = N'ONLINE'
          AND h.trangThai = N'CHO_XAC_NHAN'
          AND NOT EXISTS (
              SELECT 1 FROM ThanhToan t
              WHERE t.maHD = h.maHD
                AND t.trangThai = N'DA_THANH_TOAN'
          )
          AND (
              -- COD: chờ nhân viên xác nhận tối đa codTimeoutMinutes
              (EXISTS (SELECT 1 FROM ThanhToan t
                       WHERE t.maHD = h.maHD
                         AND t.phuongThuc = N'TIEN_MAT')
               AND h.ngayLap < DATEADD(MINUTE, -:codTimeoutMinutes, GETDATE()))
              OR
              -- Online trả trước: quá timeoutMinutes mà chưa thanh toán
              (NOT EXISTS (SELECT 1 FROM ThanhToan t
                           WHERE t.maHD = h.maHD
                             AND t.phuongThuc = N'TIEN_MAT')
               AND h.ngayLap < DATEADD(MINUTE, -:timeoutMinutes, GETDATE()))
          )
        ORDER BY h.ngayLap, h.maHD
        """, nativeQuery = true)
    List<Long> findExpiredOnlineOrderIds(
            @Param("timeoutMinutes") int timeoutMinutes,
            @Param("codTimeoutMinutes") int codTimeoutMinutes,
            @Param("batchSize") int batchSize
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(value = """
        UPDATE HoaDon
        SET trangThai = N'HUY'
        WHERE maHD = :id
          AND kenhBan = N'ONLINE'
          AND trangThai = N'CHO_XAC_NHAN'
          AND ngayLap < DATEADD(
              MINUTE,
              -:timeoutMinutes,
              GETDATE()
          )
          AND NOT EXISTS (
              SELECT 1
              FROM ThanhToan t WITH (UPDLOCK, HOLDLOCK)
              WHERE t.maHD = :id
                AND t.trangThai = N'DA_THANH_TOAN'
          )
        """, nativeQuery = true)
    int cancelExpiredOnlineOrder(
            @Param("id") Long id,
            @Param("timeoutMinutes") int timeoutMinutes
    );
    interface InvoiceHeader {

        Long getId();

        java.time.LocalDateTime getCreatedAt();

        String getCustomerName();

        String getPhone();

        String getAddress();

        String getEmployeeName();

        String getWarehouseName();

        String getChannel();

        String getStatus();

        BigDecimal getTotalAmount();

        String getNote();
    }

    @Query(
            value = """
            SELECT
                h.maHD AS id,
                h.ngayLap AS createdAt,
                COALESCE(
                    NULLIF(h.tenNguoiNhan, N''),
                    k.hoTen,
                    N'Khách vãng lai'
                ) AS customerName,
                COALESCE(
                    NULLIF(h.soDienThoaiNhan, N''),
                    k.soDienThoai
                ) AS phone,
                COALESCE(
                    NULLIF(h.diaChi, N''),
                    k.diaChi
                ) AS address,
                n.hoTen AS employeeName,
                w.tenKho AS warehouseName,
                h.kenhBan AS channel,
                h.trangThai AS status,
                h.tongTien AS totalAmount,
                h.ghiChu AS note
            FROM HoaDon h
            LEFT JOIN KhachHang k ON k.maKH = h.maKH
            LEFT JOIN NhanVien n ON n.maNV = h.maNV
            LEFT JOIN Kho w ON w.maKho = h.maKho
            WHERE (:channel = '' OR h.kenhBan = :channel)
              AND (:status = '' OR h.trangThai = :status)
              AND (:fromDate IS NULL OR h.ngayLap >= :fromDate)
              AND (:toExclusive IS NULL OR h.ngayLap < :toExclusive)
              AND (
                  CONCAT(N'HD', h.maHD) LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), h.maHD) LIKE :keyword ESCAPE '~'
                  OR COALESCE(
                      NULLIF(h.tenNguoiNhan, N''),
                      k.hoTen,
                      N'Khách vãng lai'
                  ) LIKE :keyword ESCAPE '~'
                  OR COALESCE(
                      NULLIF(h.soDienThoaiNhan, N''),
                      k.soDienThoai,
                      N''
                  ) LIKE :keyword ESCAPE '~'
              )
            ORDER BY
                CASE WHEN :sort = 'oldest'
                    THEN h.ngayLap END ASC,
                CASE WHEN :sort = 'amount-asc'
                    THEN h.tongTien END ASC,
                CASE WHEN :sort = 'amount-desc'
                    THEN h.tongTien END DESC,
                h.ngayLap DESC,
                h.maHD DESC
            """,
            countQuery = """
            SELECT COUNT_BIG(*)
            FROM HoaDon h
            LEFT JOIN KhachHang k ON k.maKH = h.maKH
            WHERE (:channel = '' OR h.kenhBan = :channel)
              AND (:status = '' OR h.trangThai = :status)
              AND (:fromDate IS NULL OR h.ngayLap >= :fromDate)
              AND (:toExclusive IS NULL OR h.ngayLap < :toExclusive)
              AND (
                  CONCAT(N'HD', h.maHD) LIKE :keyword ESCAPE '~'
                  OR CONVERT(NVARCHAR(30), h.maHD) LIKE :keyword ESCAPE '~'
                  OR COALESCE(
                      NULLIF(h.tenNguoiNhan, N''),
                      k.hoTen,
                      N'Khách vãng lai'
                  ) LIKE :keyword ESCAPE '~'
                  OR COALESCE(
                      NULLIF(h.soDienThoaiNhan, N''),
                      k.soDienThoai,
                      N''
                  ) LIKE :keyword ESCAPE '~'
              )
            """,
            nativeQuery = true
    )
    org.springframework.data.domain.Page<InvoiceHeader> searchInvoices(
            @Param("keyword") String keyword,
            @Param("channel") String channel,
            @Param("status") String status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toExclusive") java.time.LocalDateTime toExclusive,
            @Param("sort") String sort,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = """
        SELECT
            h.maHD AS id,
            h.ngayLap AS createdAt,
            COALESCE(
                NULLIF(h.tenNguoiNhan, N''),
                k.hoTen,
                N'Khách vãng lai'
            ) AS customerName,
            COALESCE(
                NULLIF(h.soDienThoaiNhan, N''),
                k.soDienThoai
            ) AS phone,
            COALESCE(
                NULLIF(h.diaChi, N''),
                k.diaChi
            ) AS address,
            n.hoTen AS employeeName,
            w.tenKho AS warehouseName,
            h.kenhBan AS channel,
            h.trangThai AS status,
            h.tongTien AS totalAmount,
            h.ghiChu AS note
        FROM HoaDon h
        LEFT JOIN KhachHang k ON k.maKH = h.maKH
        LEFT JOIN NhanVien n ON n.maNV = h.maNV
        LEFT JOIN Kho w ON w.maKho = h.maKho
        WHERE h.maHD = :id
        """, nativeQuery = true)
    Optional<InvoiceHeader> staffInvoice(
            @Param("id") Long id
    );

    interface InvoiceDetailLine {

        Long getProductId();

        String getProductName();

        String getProductCondition();

        String getSerialNumber();

        String getWarehouseName();

        Integer getQuantity();

        BigDecimal getPrice();

        BigDecimal getSubtotal();
    }

    @Query(value = """
        SELECT
            c.maSP AS productId,
            p.tenSP AS productName,
            p.tinhTrang AS productCondition,
            s.soSerial AS serialNumber,
            w.tenKho AS warehouseName,
            c.soLuong AS quantity,
            c.donGia AS price,
            CAST(c.soLuong AS DECIMAL(18,0))
                * c.donGia AS subtotal
        FROM ChiTietHoaDon c
        JOIN HoaDon h ON h.maHD = c.maHD
        JOIN SanPham p ON p.maSP = c.maSP
        LEFT JOIN SerialMay s ON s.maSerial = c.maSerial
        LEFT JOIN Kho w
            ON w.maKho = COALESCE(c.maKho, h.maKho)
        WHERE c.maHD = :id
        ORDER BY c.maCTHD
        """, nativeQuery = true)
    List<InvoiceDetailLine> staffInvoiceLines(
            @Param("id") Long id
    );

    interface InvoicePayment {

        Long getId();

        String getMethod();

        BigDecimal getAmount();

        String getStatus();

        java.time.LocalDateTime getPaidAt();
    }

    @Query(value = """
        SELECT
            maTT AS id,
            phuongThuc AS method,
            soTien AS amount,
            trangThai AS status,
            ngayThanhToan AS paidAt
        FROM ThanhToan
        WHERE maHD = :id
        ORDER BY maTT
        """, nativeQuery = true)
    List<InvoicePayment> staffInvoicePayments(
            @Param("id") Long id
    );
    @Modifying
    @Query(
            value = """
                UPDATE ThanhToan
                SET trangThai = N'DA_THANH_TOAN',
                    ngayThanhToan = GETDATE()
                WHERE maHD = :id
                  AND phuongThuc = N'TIEN_MAT'
                  AND trangThai = N'CHUA_THANH_TOAN'
                """,
            nativeQuery = true
    )
    int markCodPaid(@Param("id") Long id);

    // Cổng thanh toán: ghi nhận thanh toán online thành công
    @Modifying
    @Query(
            value = """
                UPDATE t
                SET trangThai = N'DA_THANH_TOAN',
                    ngayThanhToan = GETDATE()
                FROM ThanhToan t
                JOIN HoaDon h ON h.maHD = t.maHD
                WHERE h.maHD = :id
                  AND h.kenhBan = N'ONLINE'
                  AND h.trangThai = N'CHO_XAC_NHAN'
                  AND t.trangThai = N'CHUA_THANH_TOAN'
                  AND t.phuongThuc IN (N'CHUYEN_KHOAN', N'MOMO', N'ZALOPAY')
                  AND t.soTien = h.tongTien
                  AND h.tongTien > 0
                """,
            nativeQuery = true
    )
    int markOnlinePaid(@Param("id") Long id);

    @Modifying
    @Query(
            value = """
                UPDATE HoaDon
                SET trangThai = N'DA_XAC_NHAN'
                WHERE maHD = :id
                  AND kenhBan = N'ONLINE'
                  AND trangThai = N'CHO_XAC_NHAN'
                """,
            nativeQuery = true
    )
    int autoConfirmOnline(@Param("id") Long id);

    // VNPAY: lưu mã giao dịch (vnp_TransactionNo) sau khi thanh toán thành công
    @Modifying
    @Query(
            value = """
                UPDATE ThanhToan
                SET maGiaoDich = :code
                WHERE maHD = :id
                  AND trangThai = N'DA_THANH_TOAN'
                  AND maGiaoDich IS NULL
                """,
            nativeQuery = true
    )
    int saveTransactionCode(
            @Param("id") Long id,
            @Param("code") String code
    );
    // VNPAY đã thu tiền nhưng đơn đã hủy/hết hạn: đánh dấu "cần hoàn tiền" lên ghi chú đơn.
    // - Dùng CHARINDEX thay vì LIKE vì dấu [ ] là ký tự đặc biệt của LIKE.
    //   Nhờ đó return URL và IPN cùng gọi vẫn chỉ ghi một lần.
    // - LEFT(..., 255) vì cột ghiChu chỉ dài 255 ký tự (tránh lỗi truncated).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                UPDATE HoaDon
                SET ghiChu = LEFT(
                    CONCAT(
                        :note,
                        CASE WHEN ghiChu IS NULL OR ghiChu = N''
                             THEN N''
                             ELSE CONCAT(N' | Ghi chú của khách: ', ghiChu)
                        END
                    ), 255)
                WHERE maHD = :id
                  AND kenhBan = N'ONLINE'
                  AND (ghiChu IS NULL OR CHARINDEX(:marker, ghiChu) = 0)
                """,
            nativeQuery = true
    )
    int markRefundNeeded(
            @Param("id") Long id,
            @Param("note") String note,
            @Param("marker") String marker
    );
}
