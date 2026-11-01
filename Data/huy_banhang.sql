-- =====================================================================
-- Người làm : Huy
-- Phần      : Bán hàng  (HoaDon, ChiTietHoaDon, ThanhToan)
-- Trigger   : TRIGGER_ChiTietHoaDon_After_Insert
--             TRIGGER_HoaDon_After_Update
-- DBMS      : MySQL 8.0.16+  (cần >= 8.0.16 để CHECK có hiệu lực)
-- Lưu ý     : Chạy SAU KHI các bảng cha đã tồn tại:
--             KhachHang, NhanVien, Kho, SanPham, SerialMay, TonKho, BienDongKho
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. BẢNG HoaDon
-- ---------------------------------------------------------------------
CREATE TABLE HoaDon (
    maHD      BIGINT        NOT NULL AUTO_INCREMENT,
    maKH      BIGINT        NULL,
    maNV      BIGINT        NULL,
    maKho     BIGINT        NOT NULL,
    ngayLap   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kenhBan   VARCHAR(20)   NOT NULL,
    trangThai VARCHAR(20)   NOT NULL DEFAULT 'CHO_XAC_NHAN',
    tongTien  DECIMAL(18,0) NOT NULL DEFAULT 0,
    diaChi    NVARCHAR(255) NULL,
    ghiChu    NVARCHAR(255) NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY (maHD),
    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH)  REFERENCES KhachHang (maKH),
    CONSTRAINT FK_HoaDon_NhanVien  FOREIGN KEY (maNV)  REFERENCES NhanVien (maNV),
    CONSTRAINT FK_HoaDon_Kho       FOREIGN KEY (maKho) REFERENCES Kho (maKho),
    CONSTRAINT CK_HoaDon_kenhBan   CHECK (kenhBan IN ('QUAY','ONLINE')),
    CONSTRAINT CK_HoaDon_trangThai CHECK (trangThai IN
        ('CHO_XAC_NHAN','DA_XAC_NHAN','DANG_GIAO','HOAN_TAT','HUY')),
    CONSTRAINT CK_HoaDon_tongTien  CHECK (tongTien >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. BẢNG ChiTietHoaDon
-- ---------------------------------------------------------------------
CREATE TABLE ChiTietHoaDon (
    maCTHD   BIGINT        NOT NULL AUTO_INCREMENT,
    maHD     BIGINT        NOT NULL,
    maSP     BIGINT        NOT NULL,
    maSerial BIGINT        NULL,          -- chỉ có khi bán máy có serial
    soLuong  INT           NOT NULL DEFAULT 1,
    donGia   DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maCTHD),
    CONSTRAINT FK_CTHD_HoaDon    FOREIGN KEY (maHD)     REFERENCES HoaDon (maHD),
    CONSTRAINT FK_CTHD_SanPham   FOREIGN KEY (maSP)     REFERENCES SanPham (maSP),
    CONSTRAINT FK_CTHD_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay (maSerial),
    CONSTRAINT CK_CTHD_soLuong   CHECK (soLuong > 0),
    CONSTRAINT CK_CTHD_donGia    CHECK (donGia >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. BẢNG ThanhToan
-- ---------------------------------------------------------------------
CREATE TABLE ThanhToan (
    maTT          BIGINT        NOT NULL AUTO_INCREMENT,
    maHD          BIGINT        NOT NULL,
    phuongThuc    VARCHAR(20)   NOT NULL,
    soTien        DECIMAL(18,0) NOT NULL,
    trangThai     VARCHAR(20)   NOT NULL DEFAULT 'CHUA_THANH_TOAN',
    ngayThanhToan DATETIME      NULL,
    CONSTRAINT PK_ThanhToan PRIMARY KEY (maTT),
    CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon (maHD),
    CONSTRAINT CK_ThanhToan_phuongThuc CHECK (phuongThuc IN ('TIEN_MAT','CHUYEN_KHOAN','COD')),
    CONSTRAINT CK_ThanhToan_trangThai  CHECK (trangThai IN ('CHUA_THANH_TOAN','DA_THANH_TOAN')),
    CONSTRAINT CK_ThanhToan_soTien     CHECK (soTien >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. TRIGGER: thêm dòng chi tiết hóa đơn  ->  bán hàng
--    - kiểm tra đủ tồn kho (không đủ thì báo lỗi, MySQL tự rollback câu INSERT)
--    - trừ TonKho
--    - ghi BienDongKho loại BAN
--    - nếu có serial thì đổi SerialMay.trangThai = DA_BAN
--    - cộng vào HoaDon.tongTien
-- ---------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER TRIGGER_ChiTietHoaDon_After_Insert
AFTER INSERT ON ChiTietHoaDon
FOR EACH ROW
BEGIN
    DECLARE v_maKho BIGINT;
    DECLARE v_maNV  BIGINT;
    DECLARE v_ton   INT;

    SELECT maKho, maNV INTO v_maKho, v_maNV
    FROM HoaDon WHERE maHD = NEW.maHD;

    SELECT soLuong INTO v_ton
    FROM TonKho
    WHERE maKho = v_maKho AND maSP = NEW.maSP;

    IF v_ton IS NULL OR v_ton < NEW.soLuong THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Khong du ton kho de ban';
    END IF;

    UPDATE TonKho
    SET soLuong = soLuong - NEW.soLuong
    WHERE maKho = v_maKho AND maSP = NEW.maSP;

    INSERT INTO BienDongKho (maKho, maSP, maNV, maCTHD, loaiBienDong, soLuong, ngayBienDong)
    VALUES (v_maKho, NEW.maSP, v_maNV, NEW.maCTHD, 'BAN', NEW.soLuong, NOW());

    IF NEW.maSerial IS NOT NULL THEN
        UPDATE SerialMay SET trangThai = 'DA_BAN' WHERE maSerial = NEW.maSerial;
    END IF;

    UPDATE HoaDon
    SET tongTien = tongTien + NEW.soLuong * NEW.donGia
    WHERE maHD = NEW.maHD;
END$$

-- ---------------------------------------------------------------------
-- 5. TRIGGER: cập nhật hóa đơn -> nếu chuyển sang HUY thì hoàn kho
--    - cộng lại TonKho
--    - ghi BienDongKho loại HOAN_HUY
--    - trả serial về TRONG_KHO
--    (chỉ chạy khi trạng thái ĐỔI sang HUY, tránh chạy lặp)
-- ---------------------------------------------------------------------
CREATE TRIGGER TRIGGER_HoaDon_After_Update
AFTER UPDATE ON HoaDon
FOR EACH ROW
BEGIN
    IF OLD.trangThai <> 'HUY' AND NEW.trangThai = 'HUY' THEN

        UPDATE TonKho t
        JOIN (SELECT maSP, SUM(soLuong) AS sl
              FROM ChiTietHoaDon
              WHERE maHD = NEW.maHD
              GROUP BY maSP) c ON c.maSP = t.maSP
        SET t.soLuong = t.soLuong + c.sl
        WHERE t.maKho = NEW.maKho;

        INSERT INTO BienDongKho (maKho, maSP, maNV, maCTHD, loaiBienDong, soLuong, ngayBienDong)
        SELECT NEW.maKho, maSP, NEW.maNV, maCTHD, 'HOAN_HUY', soLuong, NOW()
        FROM ChiTietHoaDon
        WHERE maHD = NEW.maHD;

        UPDATE SerialMay s
        JOIN ChiTietHoaDon c ON c.maSerial = s.maSerial
        SET s.trangThai = 'TRONG_KHO'
        WHERE c.maHD = NEW.maHD;
    END IF;
END$$

DELIMITER ;
