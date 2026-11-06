Create database QLStoreConsole;
use QLStoreConsole;

-- ==================== Thanh Ngân (Nhân viên, Khách hàng, Tài khoản, Nhà cung cấp, Serial máy) ===========================================
--  NhanVien
CREATE TABLE NhanVien (
    maNV BIGINT AUTO_INCREMENT PRIMARY KEY,
    hoTen VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(10) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    diaChi VARCHAR(255) NULL,
    chucVu VARCHAR(100) NULL,
    ngayVaoLam DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    daNghiViec BIT(1) NOT NULL DEFAULT b'0'
);
-- KhachHang
CREATE TABLE KhachHang (
    maKH BIGINT AUTO_INCREMENT PRIMARY KEY,
    hoTen VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(10)  NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    diaChi VARCHAR(255) NULL,
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- TaiKhoan 
CREATE TABLE TaiKhoan (
    maTK BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenDangNhap VARCHAR(100) NOT NULL UNIQUE,
    matKhauMaHoa VARCHAR(255) NOT NULL,
    vaiTro VARCHAR(20)  NOT NULL,
    maNV BIGINT NULL,
    maKH BIGINT NULL,
    biKhoa BIT(1) NOT NULL DEFAULT b'0',
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT CK_TaiKhoan_vaiTro
        CHECK (vaiTro IN ('ADMIN', 'NHANVIEN', 'KHACHHANG')),
    CONSTRAINT CK_TaiKhoan_NV_KH
        CHECK (maNV IS NULL OR maKH IS NULL),
    CONSTRAINT FK_TaiKhoan_NhanVien
        FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_TaiKhoan_KhachHang
        FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
) ;
-- NhaCungCap
CREATE TABLE NhaCungCap (
    maNCC BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenNCC VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(10)  NULL,
    email VARCHAR(100) NULL,
    diaChi VARCHAR(255) NULL,
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- SerialMay
CREATE TABLE SerialMay (
    maSerial  BIGINT AUTO_INCREMENT PRIMARY KEY,
    maSP BIGINT NOT NULL,
    soSerial VARCHAR(100) NOT NULL UNIQUE,
    tinhTrang VARCHAR(20) NOT NULL DEFAULT 'MOI',
    trangThai VARCHAR(20) NOT NULL DEFAULT 'TRONG_KHO',
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT CK_SerialMay_tinhTrang
        CHECK (tinhTrang IN ('MOI', 'CU')),
    CONSTRAINT CK_SerialMay_trangThai
        CHECK (trangThai IN ('TRONG_KHO', 'DA_BAN', 'BAO_HANH')),
    CONSTRAINT FK_SerialMay_SanPham
        FOREIGN KEY (maSP) REFERENCES SanPham(maSP)
); 
-- ==================================================================================================


-- ==============================Phượng Vĩ( Phiếu Thu cũ, chi tiết phiếu Thu cũ, phiếu Bảo hành)============

-- 1 Phiếu thu cũ
CREATE TABLE PhieuThuCu (
    maPTC        BIGINT AUTO_INCREMENT PRIMARY KEY,
    maKH         BIGINT        NOT NULL,
    maNV         BIGINT        NOT NULL,
    maHD         BIGINT        NULL,
    maKho        BIGINT        NOT NULL,
    ngayThu      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tongTienThu  DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu       NVARCHAR(255)  NULL,

    CONSTRAINT FK_PhieuThuCu_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
    CONSTRAINT FK_PhieuThuCu_NhanVien  FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PhieuThuCu_HoaDon    FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
    CONSTRAINT FK_PhieuThuCu_Kho       FOREIGN KEY (maKho) REFERENCES Kho(maKho)
);

--  Chi tiết phiếu thu cũ
CREATE TABLE ChiTietPhieuThuCu (
    maCTTC     BIGINT AUTO_INCREMENT PRIMARY KEY,
    maPTC      BIGINT        NOT NULL,
    maSP       BIGINT        NOT NULL,
    soLuong    INT           NOT NULL,
    donGiaThu  DECIMAL(18,0) NOT NULL,
    tinhTrang  NVARCHAR(100)   NOT NULL DEFAULT 'CU',
    ghiChu     VARCHAR(255)  NULL,

    CONSTRAINT FK_ChiTietPhieuThuCu_PhieuThuCu FOREIGN KEY (maPTC) REFERENCES PhieuThuCu(maPTC),
    CONSTRAINT FK_ChiTietPhieuThuCu_SanPham    FOREIGN KEY (maSP)  REFERENCES SanPham(maSP),

    CONSTRAINT CK_ChiTietPhieuThuCu_SoLuong   CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietPhieuThuCu_DonGiaThu CHECK (donGiaThu >= 0)
);

-- 3 Phiếu bảo hành
CREATE TABLE PhieuBaoHanh (
    maBH BIGINT AUTO_INCREMENT PRIMARY KEY,
    maSerial BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    ngayTiepNhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trangThaiBaoHanh BIT(1) NOT NULL DEFAULT b'0', -- b'0' là ngoài bảo hành, b'1' là trong bảo hành,
    ngayHenTra DATETIME NULL,
    noiDung NVARCHAR(255) NULL,
    moTaLoi NVARCHAR(255) NULL,
    chiPhi DECIMAL(18,0) NOT NULL DEFAULT 0,
    trangThai VARCHAR(20) NOT NULL DEFAULT 'TIEP_NHAN',

    CONSTRAINT FK_PhieuBaoHanh_SerialMay
        FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial),

    CONSTRAINT FK_PhieuBaoHanh_NhanVien
        FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),

    CONSTRAINT CK_PhieuBaoHanh_TrangThai
        CHECK (trangThai IN ('TIEP_NHAN', 'DANG_XU_LY', 'HOAN_TAT')),

    CONSTRAINT CK_PhieuBaoHanh_ChiPhi
        CHECK (chiPhi >= 0)
);
-- ============================================================================================================

-- ==================== Hữu Nhật (LoaiSanPham, NenTang, SanPham, Kho, TonKho,BienDongKho) ===========================================
CREATE TABLE LoaiSanPham (
    maLoai BIGINT NOT NULL AUTO_INCREMENT,
    tenLoai VARCHAR(100) NOT NULL,
    CONSTRAINT PK_LoaiSanPham PRIMARY KEY (maLoai),
    CONSTRAINT UQ_LoaiSanPham_tenLoai UNIQUE (tenLoai)
);

CREATE TABLE NenTang (
    maNenTang BIGINT NOT NULL AUTO_INCREMENT,
    tenNenTang VARCHAR(100) NOT NULL,
    CONSTRAINT PK_NenTang PRIMARY KEY (maNenTang),
    CONSTRAINT UQ_NenTang_tenNenTang UNIQUE (tenNenTang)
);

CREATE TABLE Kho (
    maKho BIGINT NOT NULL AUTO_INCREMENT,
    tenKho VARCHAR(100) NOT NULL,
    diaChi VARCHAR(255) NULL,
    CONSTRAINT PK_Kho PRIMARY KEY (maKho)
);

CREATE TABLE SanPham (
    maSP BIGINT NOT NULL AUTO_INCREMENT,
    tenSP VARCHAR(100) NOT NULL,
    moTa VARCHAR(255) NULL,
    giaBan DECIMAL(18,0) NOT NULL,
    hinhAnh VARCHAR(255) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    maLoai BIGINT NOT NULL,
    maNenTang BIGINT NULL,
    CONSTRAINT PK_SanPham PRIMARY KEY (maSP),
    CONSTRAINT CK_SanPham_giaBan CHECK (giaBan > 0),
    CONSTRAINT CK_SanPham_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT FK_SanPham_LoaiSanPham FOREIGN KEY (maLoai) REFERENCES LoaiSanPham (maLoai),
    CONSTRAINT FK_SanPham_NenTang FOREIGN KEY (maNenTang) REFERENCES NenTang (maNenTang)
);

CREATE TABLE TonKho (
    maTon BIGINT NOT NULL AUTO_INCREMENT,
    soLuong INT NOT NULL DEFAULT 0,
    maKho BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    CONSTRAINT PK_TonKho PRIMARY KEY (maTon),
    CONSTRAINT CK_TonKho_soLuong CHECK (soLuong >= 0),
    CONSTRAINT UQ_TonKho_maKho_maSP UNIQUE (maKho, maSP),
    CONSTRAINT FK_TonKho_Kho FOREIGN KEY (maKho) REFERENCES Kho (maKho),
    CONSTRAINT FK_TonKho_SanPham FOREIGN KEY (maSP) REFERENCES SanPham (maSP)
);

CREATE TABLE BienDongKho (
    maBD BIGINT NOT NULL AUTO_INCREMENT,
    loaiBienDong VARCHAR(20) NOT NULL,
    soLuong INT NOT NULL,
    soLuongTruoc INT NOT NULL,
    soLuongSau INT NOT NULL,
    thoiGian DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maKho BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    maNV BIGINT NULL,
    maCTPN BIGINT NULL,
    maCTHD BIGINT NULL,
    maCTTC BIGINT NULL,
    CONSTRAINT PK_BienDongKho PRIMARY KEY (maBD),
    CONSTRAINT CK_BienDongKho_loaiBienDong CHECK (loaiBienDong IN ('NHAP', 'BAN', 'THU_CU', 'HOAN_HUY')),
    CONSTRAINT CK_BienDongKho_soLuong CHECK (soLuong > 0),
    CONSTRAINT CK_BienDongKho_soLuongTruoc CHECK (soLuongTruoc >= 0),
    CONSTRAINT CK_BienDongKho_soLuongSau CHECK (
        soLuongSau >= 0
        AND (
            (loaiBienDong = 'BAN' AND soLuongSau = soLuongTruoc - soLuong)
            OR (loaiBienDong <> 'BAN' AND soLuongSau = soLuongTruoc + soLuong)
        )
    ),
    CONSTRAINT CK_BienDongKho_chungTu CHECK (
        (maCTPN IS NOT NULL) + (maCTHD IS NOT NULL) + (maCTTC IS NOT NULL) = 1
    ),
    CONSTRAINT CK_BienDongKho_loaiVaChungTu CHECK (
        (loaiBienDong = 'NHAP' AND maCTPN IS NOT NULL)
        OR (loaiBienDong = 'THU_CU' AND maCTTC IS NOT NULL)
        OR (loaiBienDong IN ('BAN', 'HOAN_HUY') AND maCTHD IS NOT NULL)
    ),
    CONSTRAINT FK_BienDongKho_Kho FOREIGN KEY (maKho) REFERENCES Kho (maKho),
    CONSTRAINT FK_BienDongKho_SanPham FOREIGN KEY (maSP) REFERENCES SanPham (maSP),
    CONSTRAINT FK_BienDongKho_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien (maNV),
    CONSTRAINT FK_BienDongKho_ChiTietPhieuNhap FOREIGN KEY (maCTPN) REFERENCES ChiTietPhieuNhap (maCTPN),
    CONSTRAINT FK_BienDongKho_ChiTietHoaDon FOREIGN KEY (maCTHD) REFERENCES ChiTietHoaDon (maCTHD),
    CONSTRAINT FK_BienDongKho_ChiTietPhieuThuCu FOREIGN KEY (maCTTC) REFERENCES ChiTietPhieuThuCu (maCTTC)
);

CREATE INDEX IX_BienDongKho_maSP_thoiGian ON BienDongKho (maSP, thoiGian);

-- ============================================================================================================

-- ==================== Huy (HoaDon, ChiTietHoaDon, ThanhToan) ====================
CREATE TABLE HoaDon (
    maHD BIGINT NOT NULL AUTO_INCREMENT,
    maKH BIGINT NULL,
    maNV BIGINT NULL,
    maKho BIGINT NOT NULL,
    ngayLap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kenhBan VARCHAR(20) NOT NULL,
    trangThai VARCHAR(20) NOT NULL DEFAULT 'CHO_XAC_NHAN',
    tongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
    diaChi VARCHAR(255) NULL,
    ghiChu VARCHAR(255) NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY (maHD),
    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH)  REFERENCES KhachHang (maKH),
    CONSTRAINT FK_HoaDon_NhanVien  FOREIGN KEY (maNV)  REFERENCES NhanVien (maNV),
    CONSTRAINT FK_HoaDon_Kho       FOREIGN KEY (maKho) REFERENCES Kho (maKho),
    CONSTRAINT CK_HoaDon_kenhBan   CHECK (kenhBan IN ('QUAY', 'ONLINE')),
    CONSTRAINT CK_HoaDon_trangThai CHECK (trangThai IN
        ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DANG_GIAO', 'HOAN_TAT', 'HUY')),
    CONSTRAINT CK_HoaDon_tongTien  CHECK (tongTien >= 0)
);

CREATE TABLE ChiTietHoaDon (
    maCTHD BIGINT NOT NULL AUTO_INCREMENT,
    maHD BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    maSerial BIGINT NULL,
    soLuong INT NOT NULL DEFAULT 1,
    donGia DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maCTHD),
    CONSTRAINT FK_ChiTietHoaDon_HoaDon    FOREIGN KEY (maHD)     REFERENCES HoaDon (maHD),
    CONSTRAINT FK_ChiTietHoaDon_SanPham   FOREIGN KEY (maSP)     REFERENCES SanPham (maSP),
    CONSTRAINT FK_ChiTietHoaDon_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay (maSerial),
    CONSTRAINT CK_ChiTietHoaDon_soLuong   CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietHoaDon_donGia    CHECK (donGia >= 0)
);

CREATE TABLE ThanhToan (
    maTT BIGINT NOT NULL AUTO_INCREMENT,
    maHD BIGINT NOT NULL,
    phuongThuc VARCHAR(20) NOT NULL,
    soTien DECIMAL(18,0) NOT NULL,
    trangThai VARCHAR(20) NOT NULL DEFAULT 'CHUA_THANH_TOAN',
    ngayThanhToan DATETIME NULL,
    CONSTRAINT PK_ThanhToan PRIMARY KEY (maTT),
    CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon (maHD),
    CONSTRAINT CK_ThanhToan_phuongThuc CHECK (phuongThuc IN ('TIEN_MAT', 'CHUYEN_KHOAN', 'COD')),
    CONSTRAINT CK_ThanhToan_trangThai  CHECK (trangThai IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN')),
    CONSTRAINT CK_ThanhToan_soTien     CHECK (soTien >= 0)
);
-- ===============================================================================================

DELIMITER $$

-- ---------- Huy: TRIGGER_ChiTietHoaDon_After_Insert ----------
-- Thêm dòng chi tiết hóa đơn => bán hàng:
--   kiểm tra hóa đơn/serial/tồn kho, trừ TonKho, ghi BienDongKho 'BAN',
--   đổi SerialMay -> DA_BAN, cộng tongTien của HoaDon.
CREATE TRIGGER TRIGGER_ChiTietHoaDon_After_Insert
AFTER INSERT ON ChiTietHoaDon
FOR EACH ROW
BEGIN
    DECLARE v_maKho   BIGINT;
    DECLARE v_maNV    BIGINT;
    DECLARE v_ttHD    VARCHAR(20);
    DECLARE v_truoc   INT;
    DECLARE v_ttSeri  VARCHAR(20);
    DECLARE v_spSeri  BIGINT;

    SELECT maKho, maNV, trangThai INTO v_maKho, v_maNV, v_ttHD
    FROM HoaDon WHERE maHD = NEW.maHD;

    IF v_ttHD = 'HUY' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don da huy, khong the them chi tiet';
    END IF;

    IF NEW.maSerial IS NOT NULL THEN
        IF NEW.soLuong <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dong co serial chi duoc ban so luong 1';
        END IF;
        SELECT MAX(trangThai), MAX(maSP) INTO v_ttSeri, v_spSeri
        FROM SerialMay WHERE maSerial = NEW.maSerial;
        IF v_ttSeri IS NULL OR v_ttSeri <> 'TRONG_KHO' OR v_spSeri <> NEW.maSP THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Serial khong hop le hoac khong con trong kho';
        END IF;
    END IF;

    SELECT MAX(soLuong) INTO v_truoc
    FROM TonKho WHERE maKho = v_maKho AND maSP = NEW.maSP;

    IF v_truoc IS NULL OR v_truoc < NEW.soLuong THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong du ton kho de ban';
    END IF;

    UPDATE TonKho SET soLuong = soLuong - NEW.soLuong
    WHERE maKho = v_maKho AND maSP = NEW.maSP;

    INSERT INTO BienDongKho (loaiBienDong, soLuong, soLuongTruoc, soLuongSau, maKho, maSP, maNV, maCTHD)
    VALUES ('BAN', NEW.soLuong, v_truoc, v_truoc - NEW.soLuong, v_maKho, NEW.maSP, v_maNV, NEW.maCTHD);

    IF NEW.maSerial IS NOT NULL THEN
        UPDATE SerialMay SET trangThai = 'DA_BAN' WHERE maSerial = NEW.maSerial;
    END IF;

    UPDATE HoaDon SET tongTien = tongTien + NEW.soLuong * NEW.donGia
    WHERE maHD = NEW.maHD;
END$$

-- ----------------------------TRIGGER-----------------------------------------------
-- ---------- Huy: TRIGGER_HoaDon_After_Update ----------
-- Hóa đơn chuyển sang HUY => hoàn kho: cộng lại TonKho, ghi BienDongKho 'HOAN_HUY',
-- trả SerialMay về TRONG_KHO. Dùng con trỏ để mỗi dòng ghi đúng soLuongTruoc/soLuongSau.
CREATE TRIGGER TRIGGER_HoaDon_After_Update
AFTER UPDATE ON HoaDon
FOR EACH ROW
BEGIN
    DECLARE v_done   INT DEFAULT 0;
    DECLARE v_cthd   BIGINT;
    DECLARE v_sp     BIGINT;
    DECLARE v_sl     INT;
    DECLARE v_serial BIGINT;
    DECLARE v_truoc  INT;

    DECLARE cur CURSOR FOR
        SELECT maCTHD, maSP, soLuong, maSerial
        FROM ChiTietHoaDon WHERE maHD = NEW.maHD ORDER BY maCTHD;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    IF OLD.trangThai <> 'HUY' AND NEW.trangThai = 'HUY' THEN
        OPEN cur;
        doc_dong: LOOP
            FETCH cur INTO v_cthd, v_sp, v_sl, v_serial;
            IF v_done = 1 THEN
                LEAVE doc_dong;
            END IF;

            SELECT IFNULL(MAX(soLuong), 0) INTO v_truoc
            FROM TonKho WHERE maKho = NEW.maKho AND maSP = v_sp;

            INSERT INTO TonKho (maKho, maSP, soLuong) VALUES (NEW.maKho, v_sp, v_sl)
            ON DUPLICATE KEY UPDATE soLuong = soLuong + v_sl;

            INSERT INTO BienDongKho (loaiBienDong, soLuong, soLuongTruoc, soLuongSau, maKho, maSP, maNV, maCTHD)
            VALUES ('HOAN_HUY', v_sl, v_truoc, v_truoc + v_sl, NEW.maKho, v_sp, NEW.maNV, v_cthd);

            IF v_serial IS NOT NULL THEN
                UPDATE SerialMay SET trangThai = 'TRONG_KHO' WHERE maSerial = v_serial;
            END IF;
        END LOOP;
        CLOSE cur;
    END IF;
END$$

DELIMITER $$
CREATE TRIGGER ChiTietPhieuThuCu_After_Insert
AFTER INSERT ON ChiTietPhieuThuCu
FOR EACH ROW
BEGIN
    DECLARE v_maKho BIGINT;
    DECLARE v_maNV BIGINT;
    DECLARE v_soLuongTruoc INT DEFAULT 0;
    DECLARE v_soLuongSau   INT DEFAULT 0;

    -- 1. Lấy thông tin kho và nhân viên, khóa phiếu thu cũ
    SELECT maKho, maNV
    INTO v_maKho, v_maNV
    FROM PhieuThuCu
    WHERE maPTC = NEW.maPTC
    FOR UPDATE;
    -- 2. Cộng tổng tiền
    UPDATE PhieuThuCu
    SET tongTienThu = COALESCE(tongTienThu, 0)
                      + (NEW.soLuong * NEW.donGiaThu)
    WHERE maPTC = NEW.maPTC;
    -- 3. Đảm bảo tồn kho tồn tại
    INSERT INTO TonKho (maKho, maSP, soLuong)
    VALUES (v_maKho, NEW.maSP, 0)
    ON DUPLICATE KEY UPDATE maTon = maTon;
    -- 4. Khóa và đọc tồn kho
    SELECT soLuong
    INTO v_soLuongTruoc
    FROM TonKho
    WHERE maKho = v_maKho
      AND maSP = NEW.maSP
    FOR UPDATE;
    -- 5. Kiểm tra tràn INT
    IF v_soLuongTruoc > 2147483647 - NEW.soLuong THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ton kho vuot gioi han INT';
    END IF;
    -- 6. Tính tồn kho sau
    SET v_soLuongSau = v_soLuongTruoc + NEW.soLuong;
    -- 7. Cập nhật tồn kho
    UPDATE TonKho
    SET soLuong = v_soLuongSau
    WHERE maKho = v_maKho
      AND maSP = NEW.maSP;
    -- 8. Ghi nhận biến động kho
    INSERT INTO BienDongKho (
        loaiBienDong, soLuong, soLuongTruoc, soLuongSau,
        maKho, maSP, maNV, maCTTC
    )
    VALUES (
        'THU_CU', NEW.soLuong, v_soLuongTruoc, v_soLuongSau,
        v_maKho, NEW.maSP, v_maNV, NEW.maCTTC
    );

END$$
DELIMITER ;
-- -----------------------------------------------------------------------------------------
-- -----------------------------------------------------------------------------------------
-- ================== Thành Long (PhieuNhap, ChiTietPhieuNhap, ChiTietPhieuNhapSerial ) ===========================================
-- Table PhieuNhap,ChiTietPhieuNhap
-- Table ChiTietPhieuNhapSerial: truy lại serial nào được nhập trong dòng sản phẩm nào; 1 dòng nhập có thể có nhiều serial; mỗi serial chỉ thuộc 1 dòng nhập.
-- Trigger: khi thêm chi tiết nhập, tự cộng tổng tiền phiếu, tăng tồn kho và thêm bản ghi BienDongKho
CREATE TABLE PhieuNhap (
    maPN BIGINT NOT NULL AUTO_INCREMENT,
    maNCC BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    maKho BIGINT NOT NULL,
    ngayNhap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu VARCHAR(255) NULL,

    CONSTRAINT PK_PhieuNhap PRIMARY KEY (maPN),
    CONSTRAINT FK_PhieuNhap_NhaCungCap
        FOREIGN KEY (maNCC) REFERENCES NhaCungCap(maNCC),
    CONSTRAINT FK_PhieuNhap_NhanVien
        FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PhieuNhap_Kho
        FOREIGN KEY (maKho) REFERENCES Kho(maKho),
    CONSTRAINT CK_PhieuNhap_tongTien CHECK (tongTien >= 0)
);

CREATE TABLE ChiTietPhieuNhap (
    maCTPN BIGINT NOT NULL AUTO_INCREMENT,
    maPN BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    soLuong INT NOT NULL,
    donGiaNhap DECIMAL(18,0) NOT NULL,

    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (maCTPN),
    CONSTRAINT FK_ChiTietPhieuNhap_PhieuNhap
        FOREIGN KEY (maPN) REFERENCES PhieuNhap(maPN),
    CONSTRAINT FK_ChiTietPhieuNhap_SanPham
        FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
    CONSTRAINT CK_ChiTietPhieuNhap_soLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietPhieuNhap_donGiaNhap CHECK (donGiaNhap >= 0)
);


CREATE TABLE ChiTietPhieuNhapSerial (
    maCTPN BIGINT NOT NULL,
    maSerial BIGINT NOT NULL,

    CONSTRAINT PK_ChiTietPhieuNhapSerial
        PRIMARY KEY (maCTPN, maSerial),
    CONSTRAINT UQ_ChiTietPhieuNhapSerial_maSerial
        UNIQUE (maSerial),
    CONSTRAINT FK_CTPNSerial_ChiTietPhieuNhap
        FOREIGN KEY (maCTPN) REFERENCES ChiTietPhieuNhap(maCTPN),
    CONSTRAINT FK_CTPNSerial_SerialMay
        FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial)
);

DELIMITER $$

CREATE TRIGGER TRIGGER_ChiTietPhieuNhap_After_Insert
AFTER INSERT ON ChiTietPhieuNhap
FOR EACH ROW
BEGIN
    DECLARE v_maKho BIGINT;
    DECLARE v_maNV BIGINT;
    DECLARE v_soLuongTruoc INT DEFAULT 0;
    DECLARE v_soLuongSau INT DEFAULT 0;

    -- Lấy kho và nhân viên từ phiếu nhập
    SELECT maKho, maNV
    INTO v_maKho, v_maNV
    FROM PhieuNhap
    WHERE maPN = NEW.maPN
    FOR UPDATE;

    -- Tạo dòng tồn kho ban đầu nếu sản phẩm chưa có trong kho
    INSERT INTO TonKho (maKho, maSP, soLuong)
    VALUES (v_maKho, NEW.maSP, 0)
    ON DUPLICATE KEY UPDATE maTon = maTon;

    -- Khóa và đọc tồn kho hiện tại
    SELECT soLuong
    INTO v_soLuongTruoc
    FROM TonKho
    WHERE maKho = v_maKho
      AND maSP = NEW.maSP
    FOR UPDATE;

    -- Tránh vượt giới hạn kiểu INT của số lượng tồn
    IF v_soLuongTruoc > 2147483647 - NEW.soLuong THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ton kho vuot gioi han INT';
    END IF;

    SET v_soLuongSau = v_soLuongTruoc + NEW.soLuong;

    -- Cập nhật tồn kho
    UPDATE TonKho
    SET soLuong = v_soLuongSau
    WHERE maKho = v_maKho
      AND maSP = NEW.maSP;

    -- Cộng chi phí dòng nhập vào tổng tiền phiếu
    UPDATE PhieuNhap
    SET tongTien = tongTien + (NEW.soLuong * NEW.donGiaNhap)
    WHERE maPN = NEW.maPN;

    -- Ghi lịch sử biến động kho
    INSERT INTO BienDongKho (
        loaiBienDong,
        soLuong,
        soLuongTruoc,
        soLuongSau,
        maKho,
        maSP,
        maNV,
        maCTPN
    )
    VALUES (
        'NHAP',
        NEW.soLuong,
        v_soLuongTruoc,
        v_soLuongSau,
        v_maKho,
        NEW.maSP,
        v_maNV,
        NEW.maCTPN
    );
END$$

DELIMITER ;
-- Tổng chi phí nhập theo nhà cung cấp trong khoảng ngày.
SELECT
    ncc.maNCC,
    ncc.tenNCC,
    SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
FROM PhieuNhap pn
JOIN NhaCungCap ncc
    ON ncc.maNCC = pn.maNCC
JOIN ChiTietPhieuNhap ct
    ON ct.maPN = pn.maPN
WHERE pn.ngayNhap >= @TuNgay
  AND pn.ngayNhap < @DenNgay
GROUP BY ncc.maNCC, ncc.tenNCC
ORDER BY tongChiPhi DESC;
-- Tổng chi phí theo tháng:
SELECT
    YEAR(pn.ngayNhap) AS nam,
    MONTH(pn.ngayNhap) AS thang,
    SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
FROM PhieuNhap pn
JOIN ChiTietPhieuNhap ct
    ON ct.maPN = pn.maPN
GROUP BY YEAR(pn.ngayNhap), MONTH(pn.ngayNhap)
ORDER BY nam, thang;
--
