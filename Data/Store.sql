DROP DATABASE IF EXISTS QLStoreConsole;
CREATE DATABASE QLStoreConsole CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE QLStoreConsole;

-- ===================== DANH MỤC =====================
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

-- ===================== NHÂN VIÊN, KHÁCH HÀNG, TÀI KHOẢN, NCC =====================
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

CREATE TABLE KhachHang (
    maKH BIGINT AUTO_INCREMENT PRIMARY KEY,
    hoTen VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(10) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    diaChi VARCHAR(255) NULL,
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE TaiKhoan (
    maTK BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenDangNhap VARCHAR(100) NOT NULL UNIQUE,
    matKhau VARCHAR(255) NOT NULL,
    vaiTro VARCHAR(20) NOT NULL,
    maNV BIGINT NULL,
    maKH BIGINT NULL,
    biKhoa BIT(1) NOT NULL DEFAULT b'0',
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_TaiKhoan_maNV UNIQUE (maNV),
    CONSTRAINT UQ_TaiKhoan_maKH UNIQUE (maKH),
    CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN ('ADMIN', 'NHANVIEN', 'KHACHHANG')),
    CONSTRAINT CK_TaiKhoan_NV_KH CHECK (maNV IS NULL OR maKH IS NULL),
    CONSTRAINT CK_TaiKhoan_NV CHECK (vaiTro <> 'NHANVIEN' OR maNV IS NOT NULL),
    CONSTRAINT CK_TaiKhoan_KH CHECK (vaiTro <> 'KHACHHANG' OR maKH IS NOT NULL),
    CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_TaiKhoan_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
);

CREATE TABLE NhaCungCap (
    maNCC BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenNCC VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(10) NULL,
    email VARCHAR(100) NULL,
    diaChi VARCHAR(255) NULL,
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================== SẢN PHẨM, SERIAL, TỒN KHO =====================
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

CREATE TABLE SerialMay (
    maSerial BIGINT AUTO_INCREMENT PRIMARY KEY,
    maSP BIGINT NOT NULL,
    soSerial VARCHAR(100) NOT NULL UNIQUE,
    tinhTrang VARCHAR(20) NOT NULL DEFAULT 'MOI',
    trangThai VARCHAR(20) NOT NULL DEFAULT 'TRONG_KHO',
    ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT CK_SerialMay_tinhTrang CHECK (tinhTrang IN ('MOI', 'CU')),
    CONSTRAINT CK_SerialMay_trangThai CHECK (trangThai IN ('TRONG_KHO', 'DA_BAN', 'BAO_HANH')),
    CONSTRAINT FK_SerialMay_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP)
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

-- ===================== HÓA ĐƠN =====================
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
    CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang (maKH),
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien (maNV),
    CONSTRAINT FK_HoaDon_Kho FOREIGN KEY (maKho) REFERENCES Kho (maKho),
    CONSTRAINT CK_HoaDon_kenhBan CHECK (kenhBan IN ('QUAY', 'ONLINE')),
    CONSTRAINT CK_HoaDon_trangThai CHECK (trangThai IN
        ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DANG_GIAO', 'HOAN_TAT', 'HUY')),
    CONSTRAINT CK_HoaDon_tongTien CHECK (tongTien >= 0)
);

CREATE TABLE ChiTietHoaDon (
    maCTHD BIGINT NOT NULL AUTO_INCREMENT,
    maHD BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    maSerial BIGINT NULL,
    soLuong INT NOT NULL DEFAULT 1,
    donGia DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maCTHD),
    CONSTRAINT FK_ChiTietHoaDon_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon (maHD),
    CONSTRAINT FK_ChiTietHoaDon_SanPham FOREIGN KEY (maSP) REFERENCES SanPham (maSP),
    CONSTRAINT FK_ChiTietHoaDon_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay (maSerial),
    CONSTRAINT CK_ChiTietHoaDon_soLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietHoaDon_donGia CHECK (donGia >= 0)
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
    CONSTRAINT CK_ThanhToan_trangThai CHECK (trangThai IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN')),
    CONSTRAINT CK_ThanhToan_soTien CHECK (soTien >= 0)
);

-- ===================== NHẬP HÀNG =====================
CREATE TABLE PhieuNhap (
    maPN BIGINT NOT NULL AUTO_INCREMENT,
    maNCC BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    maKho BIGINT NOT NULL,
    ngayNhap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu VARCHAR(255) NULL,
    CONSTRAINT PK_PhieuNhap PRIMARY KEY (maPN),
    CONSTRAINT FK_PhieuNhap_NhaCungCap FOREIGN KEY (maNCC) REFERENCES NhaCungCap(maNCC),
    CONSTRAINT FK_PhieuNhap_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PhieuNhap_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
    CONSTRAINT CK_PhieuNhap_tongTien CHECK (tongTien >= 0)
);

CREATE TABLE ChiTietPhieuNhap (
    maCTPN BIGINT NOT NULL AUTO_INCREMENT,
    maPN BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    soLuong INT NOT NULL,
    donGiaNhap DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (maCTPN),
    CONSTRAINT FK_ChiTietPhieuNhap_PhieuNhap FOREIGN KEY (maPN) REFERENCES PhieuNhap(maPN),
    CONSTRAINT FK_ChiTietPhieuNhap_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
    CONSTRAINT CK_ChiTietPhieuNhap_soLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietPhieuNhap_donGiaNhap CHECK (donGiaNhap >= 0)
);

CREATE TABLE ChiTietPhieuNhapSerial (
    maCTPN BIGINT NOT NULL,
    maSerial BIGINT NOT NULL,
    CONSTRAINT PK_ChiTietPhieuNhapSerial PRIMARY KEY (maCTPN, maSerial),
    CONSTRAINT UQ_ChiTietPhieuNhapSerial_maSerial UNIQUE (maSerial),
    CONSTRAINT FK_CTPNSerial_ChiTietPhieuNhap FOREIGN KEY (maCTPN) REFERENCES ChiTietPhieuNhap(maCTPN),
    CONSTRAINT FK_CTPNSerial_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial)
);

-- ===================== THU CŨ =====================
CREATE TABLE PhieuThuCu (
    maPTC BIGINT AUTO_INCREMENT PRIMARY KEY,
    maKH BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    maHD BIGINT NULL,
    maKho BIGINT NOT NULL,
    ngayThu DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tongTienThu DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu VARCHAR(255) NULL,
    CONSTRAINT FK_PhieuThuCu_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
    CONSTRAINT FK_PhieuThuCu_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PhieuThuCu_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
    CONSTRAINT FK_PhieuThuCu_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho)
);

CREATE TABLE ChiTietPhieuThuCu (
    maCTTC BIGINT AUTO_INCREMENT PRIMARY KEY,
    maPTC BIGINT NOT NULL,
    maSP BIGINT NOT NULL,
    soLuong INT NOT NULL,
    donGiaThu DECIMAL(18,0) NOT NULL,
    tinhTrang VARCHAR(100) NOT NULL DEFAULT 'CU',
    ghiChu VARCHAR(255) NULL,
    CONSTRAINT FK_ChiTietPhieuThuCu_PhieuThuCu FOREIGN KEY (maPTC) REFERENCES PhieuThuCu(maPTC),
    CONSTRAINT FK_ChiTietPhieuThuCu_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
    CONSTRAINT CK_ChiTietPhieuThuCu_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietPhieuThuCu_DonGiaThu CHECK (donGiaThu >= 0)
);

-- ===================== BẢO HÀNH =====================
CREATE TABLE PhieuBaoHanh (
    maBH BIGINT AUTO_INCREMENT PRIMARY KEY,
    maSerial BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    ngayTiepNhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trangThaiBaoHanh BIT(1) NOT NULL DEFAULT b'0', -- b'0' ngoài bảo hành, b'1' trong bảo hành (trigger tự tính)
    ngayHenTra DATETIME NULL,
    noiDung VARCHAR(255) NULL,
    moTaLoi VARCHAR(255) NULL,
    chiPhi DECIMAL(18,0) NOT NULL DEFAULT 0,
    trangThai VARCHAR(20) NOT NULL DEFAULT 'TIEP_NHAN',
    CONSTRAINT FK_PhieuBaoHanh_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial),
    CONSTRAINT FK_PhieuBaoHanh_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT CK_PhieuBaoHanh_TrangThai CHECK (trangThai IN ('TIEP_NHAN', 'DANG_XU_LY', 'HOAN_TAT')),
    CONSTRAINT CK_PhieuBaoHanh_ChiPhi CHECK (chiPhi >= 0)
);

-- ===================== BIẾN ĐỘNG KHO =====================
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
DELIMITER $$

-- ---------- Bán hàng: thêm chi tiết hóa đơn ----------
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

-- ---------- Không cho đổi hóa đơn đã HUY sang trạng thái khác ----------
CREATE TRIGGER TRIGGER_HoaDon_Before_Update
BEFORE UPDATE ON HoaDon
FOR EACH ROW
BEGIN
    IF OLD.trangThai = 'HUY' AND NEW.trangThai <> 'HUY' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don da huy, khong the doi trang thai';
    END IF;
    IF OLD.trangThai = 'HOAN_TAT' AND NEW.trangThai NOT IN ('HOAN_TAT', 'HUY') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don da hoan tat, khong the quay lai trang thai truoc';
    END IF;
END$$

-- ---------- Hủy hóa đơn => hoàn kho ----------
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

-- ---------- Thu cũ: thêm chi tiết ----------
CREATE TRIGGER TRIGGER_ChiTietPhieuThuCu_After_Insert
AFTER INSERT ON ChiTietPhieuThuCu
FOR EACH ROW
BEGIN
    DECLARE v_maKho BIGINT;
    DECLARE v_maNV BIGINT;
    DECLARE v_soLuongTruoc INT DEFAULT 0;
    DECLARE v_soLuongSau   INT DEFAULT 0;

    SELECT maKho, maNV INTO v_maKho, v_maNV
    FROM PhieuThuCu WHERE maPTC = NEW.maPTC FOR UPDATE;

    UPDATE PhieuThuCu
    SET tongTienThu = COALESCE(tongTienThu, 0) + (NEW.soLuong * NEW.donGiaThu)
    WHERE maPTC = NEW.maPTC;

    INSERT INTO TonKho (maKho, maSP, soLuong)
    VALUES (v_maKho, NEW.maSP, 0)
    ON DUPLICATE KEY UPDATE maTon = maTon;

    SELECT soLuong INTO v_soLuongTruoc
    FROM TonKho WHERE maKho = v_maKho AND maSP = NEW.maSP FOR UPDATE;

    IF v_soLuongTruoc > 2147483647 - NEW.soLuong THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ton kho vuot gioi han INT';
    END IF;

    SET v_soLuongSau = v_soLuongTruoc + NEW.soLuong;

    UPDATE TonKho SET soLuong = v_soLuongSau
    WHERE maKho = v_maKho AND maSP = NEW.maSP;

    INSERT INTO BienDongKho (loaiBienDong, soLuong, soLuongTruoc, soLuongSau, maKho, maSP, maNV, maCTTC)
    VALUES ('THU_CU', NEW.soLuong, v_soLuongTruoc, v_soLuongSau, v_maKho, NEW.maSP, v_maNV, NEW.maCTTC);
END$$

-- ---------- Nhập hàng: thêm chi tiết ----------
CREATE TRIGGER TRIGGER_ChiTietPhieuNhap_After_Insert
AFTER INSERT ON ChiTietPhieuNhap
FOR EACH ROW
BEGIN
    DECLARE v_maKho BIGINT;
    DECLARE v_maNV BIGINT;
    DECLARE v_soLuongTruoc INT DEFAULT 0;
    DECLARE v_soLuongSau INT DEFAULT 0;

    SELECT maKho, maNV INTO v_maKho, v_maNV
    FROM PhieuNhap WHERE maPN = NEW.maPN FOR UPDATE;

    INSERT INTO TonKho (maKho, maSP, soLuong)
    VALUES (v_maKho, NEW.maSP, 0)
    ON DUPLICATE KEY UPDATE maTon = maTon;

    SELECT soLuong INTO v_soLuongTruoc
    FROM TonKho WHERE maKho = v_maKho AND maSP = NEW.maSP FOR UPDATE;

    IF v_soLuongTruoc > 2147483647 - NEW.soLuong THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ton kho vuot gioi han INT';
    END IF;

    SET v_soLuongSau = v_soLuongTruoc + NEW.soLuong;

    UPDATE TonKho SET soLuong = v_soLuongSau
    WHERE maKho = v_maKho AND maSP = NEW.maSP;

    UPDATE PhieuNhap SET tongTien = tongTien + (NEW.soLuong * NEW.donGiaNhap)
    WHERE maPN = NEW.maPN;

    INSERT INTO BienDongKho (loaiBienDong, soLuong, soLuongTruoc, soLuongSau, maKho, maSP, maNV, maCTPN)
    VALUES ('NHAP', NEW.soLuong, v_soLuongTruoc, v_soLuongSau, v_maKho, NEW.maSP, v_maNV, NEW.maCTPN);
END$$

-- ---------- [MỚI] Serial nhập phải đúng sản phẩm và không vượt số lượng ----------
CREATE TRIGGER TRIGGER_CTPNSerial_Before_Insert
BEFORE INSERT ON ChiTietPhieuNhapSerial
FOR EACH ROW
BEGIN
    DECLARE v_spCT     BIGINT;
    DECLARE v_sl       INT;
    DECLARE v_spSerial BIGINT;
    DECLARE v_daCo     INT;

    SELECT MAX(maSP), MAX(soLuong) INTO v_spCT, v_sl
    FROM ChiTietPhieuNhap WHERE maCTPN = NEW.maCTPN;

    SELECT MAX(maSP) INTO v_spSerial
    FROM SerialMay WHERE maSerial = NEW.maSerial;

    IF v_spCT IS NULL OR v_spSerial IS NULL OR v_spCT <> v_spSerial THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Serial khong thuoc san pham cua dong nhap';
    END IF;

    SELECT COUNT(*) INTO v_daCo FROM ChiTietPhieuNhapSerial WHERE maCTPN = NEW.maCTPN;
    IF v_daCo >= v_sl THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'So serial vuot qua so luong nhap';
    END IF;
END$$

-- ---------- [MỚI] Bảo hành ----------
CREATE TRIGGER TRIGGER_PhieuBaoHanh_Before_Insert
BEFORE INSERT ON PhieuBaoHanh
FOR EACH ROW
BEGIN
    DECLARE v_tt     VARCHAR(20);
    DECLARE v_ngayBan DATETIME;

    SELECT MAX(trangThai) INTO v_tt FROM SerialMay WHERE maSerial = NEW.maSerial;
    IF v_tt IS NULL OR v_tt <> 'DA_BAN' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Serial chua ban hoac dang bao hanh, khong the tiep nhan';
    END IF;

    SELECT MIN(hd.ngayLap) INTO v_ngayBan
    FROM ChiTietHoaDon ct
    JOIN HoaDon hd ON hd.maHD = ct.maHD
    WHERE ct.maSerial = NEW.maSerial AND hd.trangThai <> 'HUY';

    -- Bảo hành 12 tháng kể từ ngày lập hóa đơn
    IF v_ngayBan IS NOT NULL AND NEW.ngayTiepNhan < DATE_ADD(v_ngayBan, INTERVAL 12 MONTH) THEN
        SET NEW.trangThaiBaoHanh = 1;
    ELSE
        SET NEW.trangThaiBaoHanh = 0;
    END IF;
END$$

CREATE TRIGGER TRIGGER_PhieuBaoHanh_After_Insert
AFTER INSERT ON PhieuBaoHanh
FOR EACH ROW
BEGIN
    IF NEW.trangThai <> 'HOAN_TAT' THEN
        UPDATE SerialMay SET trangThai = 'BAO_HANH' WHERE maSerial = NEW.maSerial;
    END IF;
END$$

CREATE TRIGGER TRIGGER_PhieuBaoHanh_After_Update
AFTER UPDATE ON PhieuBaoHanh
FOR EACH ROW
BEGIN
    IF OLD.trangThai <> 'HOAN_TAT' AND NEW.trangThai = 'HOAN_TAT' THEN
        UPDATE SerialMay SET trangThai = 'DA_BAN' WHERE maSerial = NEW.maSerial;
    END IF;
END$$

-- ---------- [MỚI] Chặn sửa/xóa chi tiết chứng từ (tránh lệch kho, lệch tổng tiền) ----------
CREATE TRIGGER TRIGGER_ChiTietHoaDon_Before_Update BEFORE UPDATE ON ChiTietHoaDon
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc sua chi tiet hoa don, hay huy hoa don';
END$$
CREATE TRIGGER TRIGGER_ChiTietHoaDon_Before_Delete BEFORE DELETE ON ChiTietHoaDon
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc xoa chi tiet hoa don, hay huy hoa don';
END$$
CREATE TRIGGER TRIGGER_ChiTietPhieuNhap_Before_Update BEFORE UPDATE ON ChiTietPhieuNhap
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc sua chi tiet phieu nhap';
END$$
CREATE TRIGGER TRIGGER_ChiTietPhieuNhap_Before_Delete BEFORE DELETE ON ChiTietPhieuNhap
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc xoa chi tiet phieu nhap';
END$$
CREATE TRIGGER TRIGGER_ChiTietPhieuThuCu_Before_Update BEFORE UPDATE ON ChiTietPhieuThuCu
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc sua chi tiet phieu thu cu';
END$$
CREATE TRIGGER TRIGGER_ChiTietPhieuThuCu_Before_Delete BEFORE DELETE ON ChiTietPhieuThuCu
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong duoc xoa chi tiet phieu thu cu';
END$$

DELIMITER ;


DELIMITER $$

-- Tạo hóa đơn rỗng, trả về mã hóa đơn
CREATE PROCEDURE sp_TaoHoaDon(
    IN p_maKH BIGINT, IN p_maNV BIGINT, IN p_maKho BIGINT,
    IN p_kenhBan VARCHAR(20), IN p_diaChi VARCHAR(255), IN p_ghiChu VARCHAR(255),
    OUT p_maHD BIGINT)
BEGIN
    INSERT INTO HoaDon (maKH, maNV, maKho, kenhBan, diaChi, ghiChu)
    VALUES (p_maKH, p_maNV, p_maKho, p_kenhBan, p_diaChi, p_ghiChu);
    SET p_maHD = LAST_INSERT_ID();
END$$

-- Thêm sản phẩm vào hóa đơn (tự lấy giá bán hiện tại; trigger lo trừ kho, cộng tiền)
CREATE PROCEDURE sp_ThemChiTietHoaDon(
    IN p_maHD BIGINT, IN p_maSP BIGINT, IN p_maSerial BIGINT, IN p_soLuong INT)
BEGIN
    DECLARE v_gia DECIMAL(18,0) DEFAULT NULL;

    SELECT giaBan INTO v_gia FROM SanPham WHERE maSP = p_maSP AND deleted = 0;
    IF v_gia IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'San pham khong ton tai hoac da ngung ban';
    END IF;

    INSERT INTO ChiTietHoaDon (maHD, maSP, maSerial, soLuong, donGia)
    VALUES (p_maHD, p_maSP, p_maSerial, p_soLuong, v_gia);
END$$

-- Tạo thanh toán cho hóa đơn theo tổng tiền hiện tại
CREATE PROCEDURE sp_TaoThanhToan(IN p_maHD BIGINT, IN p_phuongThuc VARCHAR(20))
BEGIN
    DECLARE v_tong DECIMAL(18,0);
    SELECT tongTien INTO v_tong FROM HoaDon WHERE maHD = p_maHD;
    IF v_tong IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don khong ton tai';
    END IF;
    INSERT INTO ThanhToan (maHD, phuongThuc, soTien, trangThai, ngayThanhToan)
    VALUES (p_maHD, p_phuongThuc, v_tong,
            IF(p_phuongThuc = 'COD', 'CHUA_THANH_TOAN', 'DA_THANH_TOAN'),
            IF(p_phuongThuc = 'COD', NULL, NOW()));
END$$

-- Đổi trạng thái hóa đơn (gồm cả HUY => trigger tự hoàn kho)
CREATE PROCEDURE sp_CapNhatTrangThaiHoaDon(IN p_maHD BIGINT, IN p_trangThai VARCHAR(20))
BEGIN
    DECLARE v_cu VARCHAR(20);

    SELECT trangThai INTO v_cu FROM HoaDon WHERE maHD = p_maHD FOR UPDATE;
    IF v_cu IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don khong ton tai';
    END IF;
    IF v_cu IN ('HOAN_TAT', 'HUY') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hoa don da ket thuc, khong the doi trang thai';
    END IF;

    UPDATE HoaDon SET trangThai = p_trangThai WHERE maHD = p_maHD;

    IF p_trangThai = 'HOAN_TAT' THEN
        UPDATE ThanhToan
        SET trangThai = 'DA_THANH_TOAN', ngayThanhToan = IFNULL(ngayThanhToan, NOW())
        WHERE maHD = p_maHD AND phuongThuc = 'COD';
    END IF;
END$$

-- Tiếp nhận bảo hành theo số serial
CREATE PROCEDURE sp_TiepNhanBaoHanh(
    IN p_soSerial VARCHAR(100), IN p_maNV BIGINT,
    IN p_moTaLoi VARCHAR(255), IN p_ngayHenTra DATETIME)
BEGIN
    DECLARE v_maSerial BIGINT;
    SELECT MAX(maSerial) INTO v_maSerial FROM SerialMay WHERE soSerial = p_soSerial;
    IF v_maSerial IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Khong tim thay so serial';
    END IF;

    INSERT INTO PhieuBaoHanh (maSerial, maNV, ngayHenTra, moTaLoi)
    VALUES (v_maSerial, p_maNV, p_ngayHenTra, p_moTaLoi);

    SELECT LAST_INSERT_ID() AS maBH;
END$$

-- Tra cứu hạn bảo hành theo serial (12 tháng từ ngày lập hóa đơn)
CREATE PROCEDURE sp_TraCuuBaoHanh(IN p_soSerial VARCHAR(100))
BEGIN
    SELECT s.soSerial, sp.tenSP, s.trangThai AS trangThaiSerial,
           hd.maHD, hd.ngayLap AS ngayBan,
           DATE_ADD(hd.ngayLap, INTERVAL 12 MONTH) AS hanBaoHanh,
           (NOW() < DATE_ADD(hd.ngayLap, INTERVAL 12 MONTH)) AS conHan
    FROM SerialMay s
    JOIN SanPham sp ON sp.maSP = s.maSP
    LEFT JOIN ChiTietHoaDon ct ON ct.maSerial = s.maSerial
    LEFT JOIN HoaDon hd ON hd.maHD = ct.maHD AND hd.trangThai <> 'HUY'
    WHERE s.soSerial = p_soSerial;
END$$

-- Tồn kho theo kho (NULL = tất cả kho)
CREATE PROCEDURE sp_TonKhoTheoKho(IN p_maKho BIGINT)
BEGIN
    SELECT k.tenKho, sp.maSP, sp.tenSP, tk.soLuong
    FROM TonKho tk
    JOIN Kho k ON k.maKho = tk.maKho
    JOIN SanPham sp ON sp.maSP = tk.maSP
    WHERE p_maKho IS NULL OR tk.maKho = p_maKho
    ORDER BY k.maKho, sp.maSP;
END$$

-- Doanh thu theo tháng của một năm (chỉ tính hóa đơn HOAN_TAT)
CREATE PROCEDURE sp_BaoCaoDoanhThuTheoThang(IN p_nam INT)
BEGIN
    SELECT MONTH(ngayLap) AS thang, COUNT(*) AS soHoaDon, SUM(tongTien) AS doanhThu
    FROM HoaDon
    WHERE trangThai = 'HOAN_TAT' AND YEAR(ngayLap) = p_nam
    GROUP BY MONTH(ngayLap)
    ORDER BY thang;
END$$

-- Tổng chi phí nhập theo nhà cung cấp trong khoảng ngày
CREATE PROCEDURE sp_BaoCaoChiPhiNhapTheoNCC(IN p_tuNgay DATETIME, IN p_denNgay DATETIME)
BEGIN
    SELECT ncc.maNCC, ncc.tenNCC, SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
    FROM PhieuNhap pn
    JOIN NhaCungCap ncc ON ncc.maNCC = pn.maNCC
    JOIN ChiTietPhieuNhap ct ON ct.maPN = pn.maPN
    WHERE pn.ngayNhap >= p_tuNgay AND pn.ngayNhap < p_denNgay
    GROUP BY ncc.maNCC, ncc.tenNCC
    ORDER BY tongChiPhi DESC;
END$$

-- Tổng chi phí nhập theo tháng
CREATE PROCEDURE sp_BaoCaoChiPhiNhapTheoThang()
BEGIN
    SELECT YEAR(pn.ngayNhap) AS nam, MONTH(pn.ngayNhap) AS thang,
           SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
    FROM PhieuNhap pn
    JOIN ChiTietPhieuNhap ct ON ct.maPN = pn.maPN
    GROUP BY YEAR(pn.ngayNhap), MONTH(pn.ngayNhap)
    ORDER BY nam, thang;
END$$

DELIMITER ;

-- ===== Danh mục =====
INSERT INTO LoaiSanPham (tenLoai) VALUES
('Máy console'),('Đĩa game'),('Tay cầm'),('Phụ kiện'),('Thẻ nhớ');

INSERT INTO NenTang (tenNenTang) VALUES
('PlayStation'),('Xbox'),('Nintendo Switch'),('PC / Steam');

INSERT INTO Kho (tenKho, diaChi) VALUES
('Kho chính Quận 7','12 Nguyễn Thị Thập, Quận 7, TP.HCM'),
('Kho phụ kiện Quận 1','45 Lê Lợi, Quận 1, TP.HCM'),
('Kho hàng cũ Thủ Đức','88 Võ Văn Ngân, TP. Thủ Đức, TP.HCM');

-- ===== Nhân viên (20) =====
INSERT INTO NhanVien (hoTen, soDienThoai, email, diaChi, chucVu) VALUES
('Nguyễn Thanh Ngân','0901000001','ngan.nt@store.vn','Quận 7, TP.HCM','Quản lý'),
('Trần Hữu Nhật','0901000002','nhat.th@store.vn','Quận 1, TP.HCM','Thu ngân'),
('Lê Phượng Vĩ','0901000003','vi.lp@store.vn','Quận 3, TP.HCM','Bán hàng'),
('Phạm Thành Long','0901000004','long.pt@store.vn','Quận 5, TP.HCM','Bán hàng'),
('Võ Quốc Huy','0901000005','huy.vq@store.vn','Quận 7, TP.HCM','Thu ngân'),
('Đặng Minh Anh','0901000006','anh.dm@store.vn','Quận 10, TP.HCM','Bán hàng'),
('Bùi Thu Hà','0901000007','ha.bt@store.vn','Bình Thạnh, TP.HCM','Kỹ thuật'),
('Hoàng Gia Bảo','0901000008','bao.hg@store.vn','Gò Vấp, TP.HCM','Kỹ thuật'),
('Phan Ngọc Linh','0901000009','linh.pn@store.vn','Tân Bình, TP.HCM','Kỹ thuật'),
('Vũ Đức Anh','0901000010','anh.vd@store.vn','Quận 4, TP.HCM','Kỹ thuật'),
('Đỗ Khánh Vy','0901000011','vy.dk@store.vn','Quận 2, TP.HCM','Thủ kho'),
('Ngô Tuấn Kiệt','0901000012','kiet.nt@store.vn','Thủ Đức, TP.HCM','Thủ kho'),
('Dương Mỹ Duyên','0901000013','duyen.dm@store.vn','Quận 6, TP.HCM','Bán hàng'),
('Lý Hoàng Nam','0901000014','nam.lh@store.vn','Quận 8, TP.HCM','Bán hàng'),
('Trương Bảo Châu','0901000015','chau.tb@store.vn','Phú Nhuận, TP.HCM','Thu ngân'),
('Huỳnh Anh Tuấn','0901000016','tuan.ha@store.vn','Quận 12, TP.HCM','Giao hàng'),
('Mai Thanh Tùng','0901000017','tung.mt@store.vn','Hóc Môn, TP.HCM','Giao hàng'),
('Đinh Thị Hoa','0901000018','hoa.dt@store.vn','Bình Tân, TP.HCM','Kế toán'),
('Lương Minh Khoa','0901000019','khoa.lm@store.vn','Quận 11, TP.HCM','Bán hàng'),
('Tạ Quỳnh Như','0901000020','nhu.tq@store.vn','Nhà Bè, TP.HCM','Bán hàng');

-- ===== Khách hàng  =====
INSERT INTO KhachHang (hoTen, soDienThoai, email, diaChi) VALUES
('Nguyễn Văn An','0911000001','an.nv@mail.com','Quận 7, TP.HCM'),
('Trần Thị Bình','0911000002','binh.tt@mail.com','Quận 1, TP.HCM'),
('Lê Hoàng Cường','0911000003','cuong.lh@mail.com','Quận 3, TP.HCM'),
('Phạm Thị Dung','0911000004','dung.pt@mail.com','Quận 5, TP.HCM'),
('Võ Minh Đức','0911000005','duc.vm@mail.com','Quận 10, TP.HCM'),
('Đặng Thu Giang','0911000006','giang.dt@mail.com','Bình Thạnh, TP.HCM'),
('Bùi Quang Hải','0911000007','hai.bq@mail.com','Gò Vấp, TP.HCM'),
('Hoàng Thị Hạnh','0911000008','hanh.ht@mail.com','Tân Bình, TP.HCM'),
('Phan Văn Khải','0911000009','khai.pv@mail.com','Quận 4, TP.HCM'),
('Vũ Thị Lan','0911000010','lan.vt@mail.com','Quận 2, TP.HCM'),
('Đỗ Minh Long','0911000011','long.dm@mail.com','Thủ Đức, TP.HCM'),
('Ngô Thị Mai','0911000012','mai.nt@mail.com','Quận 6, TP.HCM'),
('Dương Văn Nam','0911000013','nam.dv@mail.com','Quận 8, TP.HCM'),
('Lý Thị Oanh','0911000014','oanh.lt@mail.com','Phú Nhuận, TP.HCM'),
('Trương Quốc Phong','0911000015','phong.tq@mail.com','Quận 12, TP.HCM'),
('Huỳnh Thị Quyên','0911000016','quyen.ht@mail.com','Hóc Môn, TP.HCM'),
('Mai Văn Sơn','0911000017','son.mv@mail.com','Bình Tân, TP.HCM'),
('Đinh Thị Thảo','0911000018','thao.dt@mail.com','Quận 11, TP.HCM'),
('Lương Văn Tín','0911000019','tin.lv@mail.com','Nhà Bè, TP.HCM'),
('Tạ Thị Uyên','0911000020','uyen.tt@mail.com','Cần Giờ, TP.HCM');

-- ===== Tài khoản  =====
INSERT INTO TaiKhoan (tenDangNhap, matKhau, vaiTro, maNV, maKH) VALUES
('admin','123456','ADMIN',1,NULL),
('nv02','123456','NHANVIEN',2,NULL),
('nv03','123456','NHANVIEN',3,NULL),
('nv04','123456','NHANVIEN',4,NULL),
('nv05','123456','NHANVIEN',5,NULL),
('nv06','123456','NHANVIEN',6,NULL),
('nv07','123456','NHANVIEN',7,NULL),
('nv08','123456','NHANVIEN',8,NULL),
('kh01','123456','KHACHHANG',NULL,1),
('kh02','123456','KHACHHANG',NULL,2),
('kh03','123456','KHACHHANG',NULL,3),
('kh04','123456','KHACHHANG',NULL,4),
('kh05','123456','KHACHHANG',NULL,5),
('kh06','123456','KHACHHANG',NULL,6),
('kh07','123456','KHACHHANG',NULL,7),
('kh08','123456','KHACHHANG',NULL,8),
('kh09','123456','KHACHHANG',NULL,9),
('kh10','123456','KHACHHANG',NULL,10),
('kh11','123456','KHACHHANG',NULL,11),
('kh12','123456','KHACHHANG',NULL,12);

-- ===== Nhà cung cấp  =====
INSERT INTO NhaCungCap (tenNCC, soDienThoai, email, diaChi) VALUES
('Sony Interactive Việt Nam','0281000001','sales@sony-vn.com','Quận 1, TP.HCM'),
('Microsoft Việt Nam','0281000002','sales@microsoft-vn.com','Quận 1, TP.HCM'),
('Nintendo Asia Distribution','0281000003','sales@nintendo-asia.com','Quận 3, TP.HCM'),
('Digiworld','0281000004','sales@digiworld.vn','Quận 7, TP.HCM'),
('FPT Synnex','0281000005','sales@fptsynnex.vn','Quận 7, TP.HCM'),
('Thế Giới Game','0281000006','sales@tggame.vn','Quận 10, TP.HCM'),
('Khang Gaming','0281000007','sales@khanggaming.vn','Quận 5, TP.HCM'),
('Hoàng Hà Distribution','0281000008','sales@hoangha.vn','Quận 3, TP.HCM'),
('An Phát Gaming','0281000009','sales@anphat.vn','Tân Bình, TP.HCM'),
('Phong Vũ Trading','0281000010','sales@phongvu.vn','Quận 1, TP.HCM'),
('GearVN Wholesale','0281000011','sales@gearvn.vn','Quận 10, TP.HCM'),
('Tân Long Group','0281000012','sales@tanlong.vn','Quận 5, TP.HCM'),
('Bluestar Gaming','0281000013','sales@bluestar.vn','Gò Vấp, TP.HCM'),
('Mai Nguyên Trading','0281000014','sales@mainguyen.vn','Quận 6, TP.HCM'),
('Game Zone Supply','0281000015','sales@gamezone.vn','Bình Thạnh, TP.HCM'),
('Saigon Gamer','0281000016','sales@saigongamer.vn','Quận 4, TP.HCM'),
('Hà Nội Console','0241000017','sales@hnconsole.vn','Hà Nội'),
('Đại Dương Gaming','0236000018','sales@daiduong.vn','Đà Nẵng'),
('Pixel Trade','0281000019','sales@pixeltrade.vn','Phú Nhuận, TP.HCM'),
('Nova Distribution','0281000020','sales@nova.vn','Quận 2, TP.HCM');

-- ===== Sản phẩm ( =====
-- 
INSERT INTO SanPham (tenSP, moTa, giaBan, maLoai, maNenTang) VALUES
('PlayStation 5 Standard','PS5 bản đĩa 825GB',13990000,1,1),
('PlayStation 5 Digital','PS5 bản không ổ đĩa',11990000,1,1),
('PlayStation 5 Pro','PS5 Pro 2TB',18990000,1,1),
('Xbox Series X','Xbox Series X 1TB',12990000,1,2),
('Xbox Series S','Xbox Series S 512GB',7490000,1,2),
('Nintendo Switch OLED','Switch màn hình OLED 7 inch',8490000,1,3),
('Nintendo Switch Lite','Switch Lite cầm tay',5290000,1,3),
('Steam Deck OLED','Steam Deck OLED 512GB',17990000,1,4),
('PlayStation 4 Slim','PS4 Slim 1TB',4500000,1,1),
('Nintendo Switch 2','Switch thế hệ 2',12990000,1,3),
('God of War Ragnarök (PS5)','Đĩa game PS5',1250000,2,1),
('Marvel''s Spider-Man 2 (PS5)','Đĩa game PS5',1390000,2,1),
('Zelda: Tears of the Kingdom','Đĩa game Switch',1290000,2,3),
('Mario Kart 8 Deluxe','Đĩa game Switch',1190000,2,3),
('Forza Horizon 5 (Xbox)','Đĩa game Xbox',1090000,2,2),
('Halo Infinite (Xbox)','Đĩa game Xbox',790000,2,2),
('EA Sports FC 25 (PS5)','Đĩa game PS5',1190000,2,1),
('Elden Ring (PS5)','Đĩa game PS5',990000,2,1),
('Tay cầm DualSense','Tay cầm PS5 chính hãng',1890000,3,1),
('Nintendo Switch Pro Controller','Tay cầm Switch chính hãng',1590000,3,3);

-- ===== Serial máy : mỗi console 2 serial; serial 17,18 (PS4) là hàng cũ =====
INSERT INTO SerialMay (maSP, soSerial, tinhTrang) VALUES
(1,'SN-PS5STD-0001','MOI'),(1,'SN-PS5STD-0002','MOI'),
(2,'SN-PS5DIG-0001','MOI'),(2,'SN-PS5DIG-0002','MOI'),
(3,'SN-PS5PRO-0001','MOI'),(3,'SN-PS5PRO-0002','MOI'),
(4,'SN-XSX-0001','MOI'),(4,'SN-XSX-0002','MOI'),
(5,'SN-XSS-0001','MOI'),(5,'SN-XSS-0002','MOI'),
(6,'SN-NSWOLED-0001','MOI'),(6,'SN-NSWOLED-0002','MOI'),
(7,'SN-NSWLITE-0001','MOI'),(7,'SN-NSWLITE-0002','MOI'),
(8,'SN-STEAMDECK-0001','MOI'),(8,'SN-STEAMDECK-0002','MOI'),
(9,'SN-PS4SLIM-0001','CU'),(9,'SN-PS4SLIM-0002','CU'),
(10,'SN-NSW2-0001','MOI'),(10,'SN-NSW2-0002','MOI');

-- ===== Phiếu nhập  =====
INSERT INTO PhieuNhap (maNCC, maNV, maKho, ghiChu) VALUES
(1,3,1,'Nhập PS5 Standard'),(2,4,1,'Nhập PS5 Digital'),(3,5,1,'Nhập PS5 Pro'),
(4,6,1,'Nhập Xbox Series X'),(5,2,1,'Nhập Xbox Series S'),(6,3,1,'Nhập Switch OLED'),
(7,4,1,'Nhập Switch Lite'),(8,5,1,'Nhập Steam Deck'),(9,6,1,'Nhập PS4 Slim'),
(10,2,1,'Nhập Switch 2'),(11,3,1,'Nhập God of War'),(12,4,1,'Nhập Spider-Man 2'),
(13,5,1,'Nhập Zelda TotK'),(14,6,1,'Nhập Mario Kart 8'),(15,2,1,'Nhập Forza Horizon 5'),
(16,3,1,'Nhập Halo Infinite'),(17,4,1,'Nhập EA FC 25'),(18,5,1,'Nhập Elden Ring'),
(19,6,2,'Nhập DualSense'),(20,2,2,'Nhập Pro Controller');

-- ===== Chi tiết phiếu nhập  =====
INSERT INTO ChiTietPhieuNhap (maPN, maSP, soLuong, donGiaNhap) VALUES
(1,1,2,11500000),(2,2,2,9900000),(3,3,2,16000000),(4,4,2,10800000),(5,5,2,6100000),
(6,6,2,7000000),(7,7,2,4300000),(8,8,2,15200000),(9,9,2,3300000),(10,10,2,10900000),
(11,11,30,1000000),(12,12,30,1100000),(13,13,30,1050000),(14,14,30,950000),(15,15,30,870000),
(16,16,30,620000),(17,17,30,950000),(18,18,30,790000),(19,19,20,1500000),(20,20,20,1250000);

-- ===== Serial của từng dòng nhập  =====
INSERT INTO ChiTietPhieuNhapSerial (maCTPN, maSerial) VALUES
(1,1),(1,2),(2,3),(2,4),(3,5),(3,6),(4,7),(4,8),(5,9),(5,10),
(6,11),(6,12),(7,13),(7,14),(8,15),(8,16),(9,17),(9,18),(10,19),(10,20);

-- ===== Hóa đơn  =====
INSERT INTO HoaDon (maKH, maNV, maKho, kenhBan, trangThai, diaChi, ghiChu) VALUES
(1,3,1,'QUAY','HOAN_TAT',NULL,'Mua tại quầy'),
(2,NULL,1,'ONLINE','HOAN_TAT','Quận 1, TP.HCM','Giao hàng nhanh'),
(3,4,1,'QUAY','HOAN_TAT',NULL,NULL),
(4,NULL,1,'ONLINE','HOAN_TAT','Quận 5, TP.HCM',NULL),
(5,5,1,'QUAY','HOAN_TAT',NULL,NULL),
(6,NULL,1,'ONLINE','HOAN_TAT','Bình Thạnh, TP.HCM',NULL),
(7,6,1,'QUAY','HOAN_TAT',NULL,NULL),
(8,NULL,1,'ONLINE','HOAN_TAT','Tân Bình, TP.HCM',NULL),
(9,2,1,'QUAY','HOAN_TAT',NULL,NULL),
(10,NULL,1,'ONLINE','HOAN_TAT','Quận 2, TP.HCM',NULL),
(11,3,1,'QUAY','HOAN_TAT',NULL,NULL),
(12,4,1,'QUAY','HOAN_TAT',NULL,NULL),
(13,NULL,1,'ONLINE','HOAN_TAT','Quận 8, TP.HCM',NULL),
(14,5,1,'QUAY','HOAN_TAT',NULL,NULL),
(15,NULL,1,'ONLINE','DANG_GIAO','Quận 12, TP.HCM','Đang giao'),
(16,NULL,1,'ONLINE','CHO_XAC_NHAN','Hóc Môn, TP.HCM','Chờ xác nhận'),
(17,6,1,'QUAY','HOAN_TAT',NULL,NULL),
(18,NULL,1,'ONLINE','DA_XAC_NHAN','Quận 11, TP.HCM','Đã xác nhận'),
(19,2,2,'QUAY','HOAN_TAT',NULL,NULL),
(20,3,2,'QUAY','HOAN_TAT',NULL,'Khách đổi ý, sẽ hủy');

-- ===== Chi tiết hóa đơn  =====
INSERT INTO ChiTietHoaDon (maHD, maSP, maSerial, soLuong, donGia) VALUES
(1,1,1,1,13990000),(2,2,3,1,11990000),(3,3,5,1,18990000),(4,4,7,1,12990000),
(5,5,9,1,7490000),(6,6,11,1,8490000),(7,7,13,1,5290000),(8,8,15,1,17990000),
(9,9,17,1,4500000),(10,10,19,1,12990000),
(11,11,NULL,2,1250000),(12,12,NULL,1,1390000),(13,13,NULL,3,1290000),(14,14,NULL,1,1190000),
(15,15,NULL,2,1090000),(16,16,NULL,1,790000),(17,17,NULL,1,1190000),(18,18,NULL,2,990000),
(19,19,NULL,1,1890000),(20,20,NULL,1,1590000);

-- Hủy hóa đơn 20 => trigger hoàn kho + ghi biến động HOAN_HUY
UPDATE HoaDon SET trangThai = 'HUY' WHERE maHD = 20;

-- ===== Thanh toán  lấy theo tổng tiền hóa đơn =====
INSERT INTO ThanhToan (maHD, phuongThuc, soTien, trangThai, ngayThanhToan)
SELECT maHD,
       IF(kenhBan = 'ONLINE',
          ELT(MOD(maHD,2)+1,'COD','CHUYEN_KHOAN'),
          ELT(MOD(maHD,2)+1,'TIEN_MAT','CHUYEN_KHOAN')),
       tongTien,
       IF(trangThai = 'HOAN_TAT','DA_THANH_TOAN','CHUA_THANH_TOAN'),
       IF(trangThai = 'HOAN_TAT', ngayLap, NULL)
FROM HoaDon
ORDER BY maHD;

-- ===== Phiếu thu cũ, nhập vào kho 3 =====
INSERT INTO PhieuThuCu (maKH, maNV, maHD, maKho, ghiChu) VALUES
(1,3,1,3,'Thu PS4 cũ khi mua PS5'),(2,4,2,3,'Thu Switch Lite cũ'),(3,5,3,3,'Thu Switch OLED cũ'),
(4,6,4,3,'Thu đĩa game cũ'),(5,2,5,3,'Thu đĩa game cũ'),(6,3,6,3,'Thu đĩa game cũ'),
(7,4,7,3,'Thu đĩa game cũ'),(8,5,8,3,'Thu đĩa game cũ'),(9,6,9,3,'Thu đĩa game cũ'),
(10,2,10,3,'Thu đĩa game cũ'),(11,3,NULL,3,'Thu đĩa game cũ'),(12,4,NULL,3,'Thu tay cầm cũ'),
(13,5,NULL,3,'Thu tay cầm cũ'),(14,6,NULL,3,'Thu PS4 cũ'),(15,2,NULL,3,'Thu Switch Lite cũ'),
(16,3,NULL,3,'Thu đĩa game cũ'),(17,4,NULL,3,'Thu đĩa game cũ'),(18,5,NULL,3,'Thu đĩa game cũ'),
(19,6,NULL,3,'Thu tay cầm cũ'),(20,2,NULL,3,'Thu đĩa game cũ');

-- ===== Chi tiết phiếu thu cũ  =====
INSERT INTO ChiTietPhieuThuCu (maPTC, maSP, soLuong, donGiaThu, tinhTrang, ghiChu) VALUES
(1,9,1,2000000,'CU','Máy chạy tốt, trầy nhẹ'),
(2,7,1,2500000,'CU','Còn bảo hành 3 tháng'),
(3,6,1,4500000,'CU','Máy đẹp 95%'),
(4,11,2,600000,'CU',NULL),(5,12,1,700000,'CU',NULL),(6,13,2,650000,'CU',NULL),
(7,14,1,600000,'CU',NULL),(8,15,2,450000,'CU',NULL),(9,16,1,300000,'CU',NULL),
(10,17,3,400000,'CU',NULL),(11,18,1,500000,'CU',NULL),
(12,19,2,800000,'CU','Hơi drift analog'),(13,20,1,700000,'CU',NULL),
(14,9,1,1800000,'CU','Có vết xước'),(15,7,1,2300000,'CU',NULL),
(16,11,1,550000,'CU',NULL),(17,13,1,600000,'CU',NULL),(18,17,2,380000,'CU',NULL),
(19,19,1,750000,'CU',NULL),(20,12,2,650000,'CU',NULL);

-- ===== Phiếu bảo hành =====
-- Trigger tự tính trangThaiBaoHanh và đổi trạng thái serial
INSERT INTO PhieuBaoHanh (maSerial, maNV, ngayHenTra, noiDung, moTaLoi, chiPhi, trangThai) VALUES
(1,7,DATE_ADD(NOW(),INTERVAL 7 DAY),'Vệ sinh quạt tản nhiệt','Máy ồn quạt',0,'HOAN_TAT'),
(3,8,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay cáp HDMI','Mất tín hiệu hình',0,'HOAN_TAT'),
(5,9,DATE_ADD(NOW(),INTERVAL 7 DAY),'Cập nhật firmware','Treo logo',0,'HOAN_TAT'),
(7,10,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay nguồn','Không lên nguồn',0,'HOAN_TAT'),
(9,7,DATE_ADD(NOW(),INTERVAL 7 DAY),'Vệ sinh máy','Máy nóng',0,'HOAN_TAT'),
(11,8,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay joycon','Drift analog',0,'HOAN_TAT'),
(13,9,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay pin','Pin chai nhanh',0,'HOAN_TAT'),
(15,10,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay màn hình','Sọc màn hình',0,'HOAN_TAT'),
(17,7,DATE_ADD(NOW(),INTERVAL 7 DAY),'Thay ổ cứng','Báo lỗi ổ cứng',350000,'HOAN_TAT'),
(19,8,DATE_ADD(NOW(),INTERVAL 7 DAY),'Cập nhật hệ thống','Lỗi cập nhật',0,'HOAN_TAT'),
(1,9,DATE_ADD(NOW(),INTERVAL 5 DAY),'Thay keo tản nhiệt','Máy nóng khi chơi game',0,'HOAN_TAT'),
(3,10,DATE_ADD(NOW(),INTERVAL 5 DAY),'Thay ổ đĩa','Không đọc đĩa',0,'HOAN_TAT'),
(5,7,DATE_ADD(NOW(),INTERVAL 5 DAY),'Kiểm tra cổng USB','USB không nhận',0,'HOAN_TAT'),
(7,8,DATE_ADD(NOW(),INTERVAL 5 DAY),'Thay quạt','Quạt kêu to',0,'HOAN_TAT'),
(9,9,DATE_ADD(NOW(),INTERVAL 5 DAY),'Đang kiểm tra bo mạch','Tự khởi động lại',0,'DANG_XU_LY'),
(11,10,DATE_ADD(NOW(),INTERVAL 5 DAY),'Đang chờ linh kiện','Hỏng nút bấm',0,'DANG_XU_LY'),
(13,7,DATE_ADD(NOW(),INTERVAL 5 DAY),'Đang thay cổng sạc','Không sạc được',0,'DANG_XU_LY'),
(15,8,DATE_ADD(NOW(),INTERVAL 7 DAY),'Chờ kiểm tra','Mất âm thanh',0,'TIEP_NHAN'),
(17,9,DATE_ADD(NOW(),INTERVAL 7 DAY),'Chờ kiểm tra','Không đọc đĩa',0,'TIEP_NHAN'),
(19,10,DATE_ADD(NOW(),INTERVAL 7 DAY),'Chờ kiểm tra','Lỗi wifi',0,'TIEP_NHAN');