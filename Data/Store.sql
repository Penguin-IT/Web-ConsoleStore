SET NOEXEC OFF;
GO

USE master;
GO

IF DB_ID(N'QLStoreConsole') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE QLStoreConsole');
END;
GO

USE QLStoreConsole;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
SET NOCOUNT ON;
GO

-- =========================================================
-- 1. DANH MỤC
-- =========================================================

IF OBJECT_ID(N'LoaiSanPham', N'U') IS NULL
BEGIN
    CREATE TABLE LoaiSanPham (
        maLoai BIGINT IDENTITY(1,1) NOT NULL,
        tenLoai NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_LoaiSanPham PRIMARY KEY (maLoai),
        CONSTRAINT UQ_LoaiSanPham_tenLoai UNIQUE (tenLoai)
    );
END;
GO

IF OBJECT_ID(N'NenTang', N'U') IS NULL
BEGIN
    CREATE TABLE NenTang (
        maNenTang BIGINT IDENTITY(1,1) NOT NULL,
        tenNenTang NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_NenTang PRIMARY KEY (maNenTang),
        CONSTRAINT UQ_NenTang_tenNenTang UNIQUE (tenNenTang)
    );
END;
GO

IF OBJECT_ID(N'Kho', N'U') IS NULL
BEGIN
    CREATE TABLE Kho (
        maKho BIGINT IDENTITY(1,1) NOT NULL,
        tenKho NVARCHAR(100) NOT NULL,
        diaChi NVARCHAR(255) NULL,
        CONSTRAINT PK_Kho PRIMARY KEY (maKho)
    );
END;
GO

-- =========================================================
-- 2. NHÂN VIÊN, KHÁCH HÀNG, TÀI KHOẢN, NHÀ CUNG CẤP
-- =========================================================

IF OBJECT_ID(N'NhanVien', N'U') IS NULL
BEGIN
    CREATE TABLE NhanVien (
        maNV BIGINT IDENTITY(1,1) NOT NULL,
        hoTen NVARCHAR(100) NOT NULL,
        soDienThoai NVARCHAR(10) NOT NULL,
        email NVARCHAR(100) NULL,
        diaChi NVARCHAR(255) NULL,
        chucVu NVARCHAR(100) NULL,
        ngayVaoLam DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        daNghiViec BIT NOT NULL DEFAULT 0,
        CONSTRAINT PK_NhanVien PRIMARY KEY (maNV),
        CONSTRAINT UQ_NhanVien_soDienThoai UNIQUE (soDienThoai)
    );
END;
GO

IF OBJECT_ID(N'KhachHang', N'U') IS NULL
BEGIN
    CREATE TABLE KhachHang (
        maKH BIGINT IDENTITY(1,1) NOT NULL,
        hoTen NVARCHAR(100) NOT NULL,
        soDienThoai NVARCHAR(10) NOT NULL,
        email NVARCHAR(100) NULL,
        diaChi NVARCHAR(255) NULL,
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT PK_KhachHang PRIMARY KEY (maKH),
        CONSTRAINT UQ_KhachHang_soDienThoai UNIQUE (soDienThoai)
    );
END;
GO

IF OBJECT_ID(N'TaiKhoan', N'U') IS NULL
BEGIN
    CREATE TABLE TaiKhoan (
        maTK BIGINT IDENTITY(1,1) NOT NULL,
        tenDangNhap NVARCHAR(100) NOT NULL,
        matKhau NVARCHAR(255) NOT NULL,
        vaiTro NVARCHAR(20) NOT NULL,
        maNV BIGINT NULL,
        maKH BIGINT NULL,
        biKhoa BIT NOT NULL DEFAULT 0,
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT PK_TaiKhoan PRIMARY KEY (maTK),
        CONSTRAINT UQ_TaiKhoan_tenDangNhap UNIQUE (tenDangNhap),

        CONSTRAINT CK_TaiKhoan_vaiTro CHECK (
            vaiTro IN (
                N'ADMIN', N'NHANVIEN', N'KHACHHANG',
                N'BANHANG', N'KHO', N'BAOHANH'
            )
        ),
        CONSTRAINT CK_TaiKhoan_NV_KH CHECK (maNV IS NULL OR maKH IS NULL),
        CONSTRAINT CK_TaiKhoan_NV CHECK (vaiTro <> N'NHANVIEN' OR maNV IS NOT NULL),
        CONSTRAINT CK_TaiKhoan_KH CHECK (vaiTro <> N'KHACHHANG' OR maKH IS NOT NULL),

        CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_TaiKhoan_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
    );
END;
GO

-- DB cũ: mở rộng danh sách vai trò nếu chưa có BANHANG/KHO/BAOHANH
IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = N'CK_TaiKhoan_vaiTro'
      AND parent_object_id = OBJECT_ID(N'dbo.TaiKhoan')
      AND definition NOT LIKE N'%BANHANG%'
)
BEGIN
    ALTER TABLE dbo.TaiKhoan DROP CONSTRAINT CK_TaiKhoan_vaiTro;

    ALTER TABLE dbo.TaiKhoan
    ADD CONSTRAINT CK_TaiKhoan_vaiTro CHECK (
        vaiTro IN (
            N'ADMIN', N'NHANVIEN', N'KHACHHANG',
            N'BANHANG', N'KHO', N'BAOHANH'
        )
    );
END;
GO

IF OBJECT_ID(N'NhaCungCap', N'U') IS NULL
BEGIN
    CREATE TABLE NhaCungCap (
        maNCC BIGINT IDENTITY(1,1) NOT NULL,
        tenNCC NVARCHAR(100) NOT NULL,
        soDienThoai NVARCHAR(10) NULL,
        email NVARCHAR(100) NULL,
        diaChi NVARCHAR(255) NULL,
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT PK_NhaCungCap PRIMARY KEY (maNCC)
    );
END;
GO

-- =========================================================
-- 3. MÃ GIẢM GIÁ (tạo trước HoaDon vì HoaDon có FK tới bảng này)
-- =========================================================

IF OBJECT_ID(N'MaGiamGia', N'U') IS NULL
BEGIN
    CREATE TABLE MaGiamGia (
        maCode        VARCHAR(30)   NOT NULL,
        moTa          NVARCHAR(200) NOT NULL,
        loai          VARCHAR(10)   NOT NULL,   -- PHAN_TRAM | SO_TIEN | FREESHIP
        giaTri        DECIMAL(18,0) NOT NULL DEFAULT 0,
        giamToiDa     DECIMAL(18,0) NULL,       -- chỉ dùng cho PHAN_TRAM
        donToiThieu   DECIMAL(18,0) NOT NULL DEFAULT 0,
        soLuong       INT           NULL,       -- NULL = không giới hạn
        moiKhachToiDa INT           NOT NULL DEFAULT 1,
        batDau        DATETIME      NOT NULL,
        ketThuc       DATETIME      NOT NULL,
        hienTrangChu  BIT           NOT NULL DEFAULT 1,
        hoatDong      BIT           NOT NULL DEFAULT 1,

        CONSTRAINT PK_MaGiamGia PRIMARY KEY (maCode),
        CONSTRAINT CK_MaGiamGia_loai CHECK (loai IN ('PHAN_TRAM','SO_TIEN','FREESHIP')),
        CONSTRAINT CK_MaGiamGia_giaTri CHECK (
            giaTri >= 0 AND (loai <> 'PHAN_TRAM' OR giaTri BETWEEN 1 AND 100)
        ),
        CONSTRAINT CK_MaGiamGia_thoiGian CHECK (ketThuc > batDau)
    );
END;
GO

-- =========================================================
-- 4. SẢN PHẨM, SERIAL, TỒN KHO
-- =========================================================

IF OBJECT_ID(N'SanPham', N'U') IS NULL
BEGIN
    CREATE TABLE SanPham (
        maSP BIGINT IDENTITY(1,1) NOT NULL,
        tenSP NVARCHAR(100) NOT NULL,
        moTa NVARCHAR(255) NULL,
        giaBan DECIMAL(18,0) NOT NULL,
        hinhAnh NVARCHAR(255) NULL,
        deleted BIT NOT NULL DEFAULT 0,
        tinhTrang NVARCHAR(3) NOT NULL DEFAULT N'MOI',
        maSPGoc BIGINT NULL,
        maLoai BIGINT NOT NULL,
        maNenTang BIGINT NULL,

        CONSTRAINT PK_SanPham PRIMARY KEY (maSP),
        CONSTRAINT CK_SanPham_tinhTrang CHECK (tinhTrang IN (N'MOI', N'CU')),
        CONSTRAINT CK_SanPham_giaBan CHECK (giaBan > 0),
        CONSTRAINT CK_SanPham_deleted CHECK (deleted IN (0, 1)),
        CONSTRAINT CK_SanPham_maSPGoc CHECK (
            maSPGoc IS NULL
            OR (tinhTrang = N'CU' AND maSPGoc <> maSP)
        ),

        CONSTRAINT FK_SanPham_SanPhamGoc FOREIGN KEY (maSPGoc) REFERENCES SanPham(maSP),
        CONSTRAINT FK_SanPham_LoaiSanPham FOREIGN KEY (maLoai) REFERENCES LoaiSanPham(maLoai),
        CONSTRAINT FK_SanPham_NenTang FOREIGN KEY (maNenTang) REFERENCES NenTang(maNenTang)
    );
END;
GO

IF OBJECT_ID(N'SerialMay', N'U') IS NULL
BEGIN
    CREATE TABLE SerialMay (
        maSerial BIGINT IDENTITY(1,1) NOT NULL,
        maSP BIGINT NOT NULL,
        maKho BIGINT NULL,
        soSerial NVARCHAR(100) NOT NULL,
        tinhTrang NVARCHAR(20) NOT NULL DEFAULT N'MOI',
        trangThai NVARCHAR(20) NOT NULL DEFAULT N'TRONG_KHO',
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT PK_SerialMay PRIMARY KEY (maSerial),
        CONSTRAINT UQ_SerialMay_soSerial UNIQUE (soSerial),
        CONSTRAINT CK_SerialMay_tinhTrang CHECK (tinhTrang IN (N'MOI', N'CU')),
        CONSTRAINT CK_SerialMay_trangThai CHECK (
            trangThai IN (N'TRONG_KHO', N'DA_BAN', N'BAO_HANH')
        ),
        CONSTRAINT FK_SerialMay_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
        CONSTRAINT FK_SerialMay_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho)
    );
END;
GO

IF OBJECT_ID(N'TonKho', N'U') IS NULL
BEGIN
    CREATE TABLE TonKho (
        maTon BIGINT IDENTITY(1,1) NOT NULL,
        soLuong INT NOT NULL DEFAULT 0,
        maKho BIGINT NOT NULL,
        maSP BIGINT NOT NULL,

        CONSTRAINT PK_TonKho PRIMARY KEY (maTon),
        CONSTRAINT CK_TonKho_soLuong CHECK (soLuong >= 0),
        CONSTRAINT UQ_TonKho_maKho_maSP UNIQUE (maKho, maSP),
        CONSTRAINT FK_TonKho_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
        CONSTRAINT FK_TonKho_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP)
    );
END;
GO

-- =========================================================
-- 5. HÓA ĐƠN VÀ THANH TOÁN
-- =========================================================

IF OBJECT_ID(N'HoaDon', N'U') IS NULL
BEGIN
    CREATE TABLE HoaDon (
        maHD BIGINT IDENTITY(1,1) NOT NULL,
        maKH BIGINT NULL,
        maNV BIGINT NULL,
        maKho BIGINT NULL,
        ngayLap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        kenhBan NVARCHAR(20) NOT NULL,
        trangThai NVARCHAR(20) NOT NULL DEFAULT N'CHO_XAC_NHAN',
        tongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
        tenNguoiNhan NVARCHAR(100) NULL,
        soDienThoaiNhan NVARCHAR(10) NULL,
        diaChi NVARCHAR(255) NULL,
        ghiChu NVARCHAR(255) NULL,
        maYeuCau VARCHAR(36) NULL,
        phiVanChuyen DECIMAL(18,0) NOT NULL CONSTRAINT DF_HoaDon_phiVanChuyen DEFAULT 0,
        giamGia DECIMAL(18,0) NOT NULL CONSTRAINT DF_HoaDon_giamGia DEFAULT 0,
        maGiamGia VARCHAR(30) NULL,

        CONSTRAINT PK_HoaDon PRIMARY KEY (maHD),
        CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
        CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_HoaDon_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
        CONSTRAINT FK_HoaDon_MaGiamGia FOREIGN KEY (maGiamGia) REFERENCES MaGiamGia(maCode),
        CONSTRAINT CK_HoaDon_kenhBan CHECK (kenhBan IN (N'QUAY', N'ONLINE')),
        CONSTRAINT CK_HoaDon_trangThai CHECK (
            trangThai IN (
                N'CHO_XAC_NHAN', N'DA_XAC_NHAN', N'DANG_GIAO',
                N'HOAN_TAT', N'HUY'
            )
        ),
        CONSTRAINT CK_HoaDon_tongTien CHECK (tongTien >= 0),
        CONSTRAINT CK_HoaDon_phiVanChuyen CHECK (phiVanChuyen >= 0),
        CONSTRAINT CK_HoaDon_giamGia CHECK (giamGia >= 0)
    );
END;
GO

IF OBJECT_ID(N'ChiTietHoaDon', N'U') IS NULL
BEGIN
    CREATE TABLE ChiTietHoaDon (
        maCTHD BIGINT IDENTITY(1,1) NOT NULL,
        maHD BIGINT NOT NULL,
        maSP BIGINT NOT NULL,
        maKho BIGINT NULL,
        maSerial BIGINT NULL,
        soLuong INT NOT NULL DEFAULT 1,
        donGia DECIMAL(18,0) NOT NULL,

        CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maCTHD),
        CONSTRAINT FK_ChiTietHoaDon_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
        CONSTRAINT FK_ChiTietHoaDon_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
        CONSTRAINT FK_ChiTietHoaDon_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
        CONSTRAINT FK_ChiTietHoaDon_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial),
        CONSTRAINT CK_ChiTietHoaDon_soLuong CHECK (soLuong > 0),
        CONSTRAINT CK_ChiTietHoaDon_donGia CHECK (donGia >= 0)
    );
END;
GO

IF OBJECT_ID(N'ThanhToan', N'U') IS NULL
BEGIN
    CREATE TABLE ThanhToan (
        maTT BIGINT IDENTITY(1,1) NOT NULL,
        maHD BIGINT NOT NULL,
        phuongThuc NVARCHAR(20) NOT NULL,
        soTien DECIMAL(18,0) NOT NULL,
        trangThai NVARCHAR(20) NOT NULL DEFAULT N'CHUA_THANH_TOAN',
        ngayThanhToan DATETIME NULL,
        maGiaoDich NVARCHAR(50) NULL,
        maNVDuyet BIGINT NULL,

        CONSTRAINT PK_ThanhToan PRIMARY KEY (maTT),
        CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
        CONSTRAINT FK_ThanhToan_NVDuyet FOREIGN KEY (maNVDuyet) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_ThanhToan_phuongThuc CHECK (
            phuongThuc IN (N'TIEN_MAT', N'CHUYEN_KHOAN', N'MOMO', N'ZALOPAY')
        ),
        CONSTRAINT CK_ThanhToan_trangThai CHECK (
            trangThai IN (N'CHUA_THANH_TOAN', N'DA_THANH_TOAN')
        ),
        CONSTRAINT CK_ThanhToan_soTien CHECK (soTien >= 0)
    );
END;
GO

-- =========================================================
-- 6. NHẬP HÀNG
-- =========================================================

IF OBJECT_ID(N'PhieuNhap', N'U') IS NULL
BEGIN
    CREATE TABLE PhieuNhap (
        maPN BIGINT IDENTITY(1,1) NOT NULL,
        maNCC BIGINT NOT NULL,
        maNV BIGINT NOT NULL,
        maKho BIGINT NOT NULL,
        ngayNhap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        tongTien DECIMAL(18,0) NOT NULL DEFAULT 0,
        ghiChu NVARCHAR(255) NULL,

        CONSTRAINT PK_PhieuNhap PRIMARY KEY (maPN),
        CONSTRAINT FK_PhieuNhap_NhaCungCap FOREIGN KEY (maNCC) REFERENCES NhaCungCap(maNCC),
        CONSTRAINT FK_PhieuNhap_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_PhieuNhap_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
        CONSTRAINT CK_PhieuNhap_tongTien CHECK (tongTien >= 0)
    );
END;
GO

IF OBJECT_ID(N'ChiTietPhieuNhap', N'U') IS NULL
BEGIN
    CREATE TABLE ChiTietPhieuNhap (
        maCTPN BIGINT IDENTITY(1,1) NOT NULL,
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
END;
GO

IF OBJECT_ID(N'ChiTietPhieuNhapSerial', N'U') IS NULL
BEGIN
    CREATE TABLE ChiTietPhieuNhapSerial (
        maCTPN BIGINT NOT NULL,
        maSerial BIGINT NOT NULL,

        CONSTRAINT PK_ChiTietPhieuNhapSerial PRIMARY KEY (maCTPN, maSerial),
        CONSTRAINT UQ_ChiTietPhieuNhapSerial_maSerial UNIQUE (maSerial),
        CONSTRAINT FK_CTPNSerial_ChiTietPhieuNhap FOREIGN KEY (maCTPN) REFERENCES ChiTietPhieuNhap(maCTPN),
        CONSTRAINT FK_CTPNSerial_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial)
    );
END;
GO

-- =========================================================
-- 7. THU MUA THIẾT BỊ CŨ
-- =========================================================

IF OBJECT_ID(N'PhieuThuCu', N'U') IS NULL
BEGIN
    CREATE TABLE PhieuThuCu (
        maPTC BIGINT IDENTITY(1,1) NOT NULL,
        maKH BIGINT NOT NULL,
        maNV BIGINT NOT NULL,
        maHD BIGINT NULL,
        maKho BIGINT NOT NULL,
        ngayThu DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        tongTienThu DECIMAL(18,0) NOT NULL DEFAULT 0,
        ghiChu NVARCHAR(255) NULL,

        CONSTRAINT PK_PhieuThuCu PRIMARY KEY (maPTC),
        CONSTRAINT FK_PhieuThuCu_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
        CONSTRAINT FK_PhieuThuCu_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_PhieuThuCu_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
        CONSTRAINT FK_PhieuThuCu_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho)
    );
END;
GO

IF OBJECT_ID(N'ChiTietPhieuThuCu', N'U') IS NULL
BEGIN
    CREATE TABLE ChiTietPhieuThuCu (
        maCTTC BIGINT IDENTITY(1,1) NOT NULL,
        maPTC BIGINT NOT NULL,
        maSP BIGINT NOT NULL,
        soLuong INT NOT NULL,
        donGiaThu DECIMAL(18,0) NOT NULL,
        tinhTrang NVARCHAR(100) NOT NULL DEFAULT N'CU',
        ghiChu NVARCHAR(255) NULL,

        CONSTRAINT PK_ChiTietPhieuThuCu PRIMARY KEY (maCTTC),
        CONSTRAINT FK_ChiTietPhieuThuCu_PhieuThuCu FOREIGN KEY (maPTC) REFERENCES PhieuThuCu(maPTC),
        CONSTRAINT FK_ChiTietPhieuThuCu_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
        CONSTRAINT CK_ChiTietPhieuThuCu_SoLuong CHECK (soLuong > 0),
        CONSTRAINT CK_ChiTietPhieuThuCu_DonGiaThu CHECK (donGiaThu >= 0)
    );
END;
GO

-- =========================================================
-- 8. BẢO HÀNH
-- =========================================================

IF OBJECT_ID(N'PhieuBaoHanh', N'U') IS NULL
BEGIN
    CREATE TABLE PhieuBaoHanh (
        maBH BIGINT IDENTITY(1,1) NOT NULL,
        maSerial BIGINT NOT NULL,
        maNV BIGINT NOT NULL,
        ngayTiepNhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        trangThaiBaoHanh BIT NOT NULL DEFAULT 0,
        ngayHenTra DATETIME NULL,
        noiDung NVARCHAR(255) NULL,
        moTaLoi NVARCHAR(255) NULL,
        chiPhi DECIMAL(18,0) NOT NULL DEFAULT 0,
        trangThai NVARCHAR(20) NOT NULL DEFAULT N'TIEP_NHAN',

        CONSTRAINT PK_PhieuBaoHanh PRIMARY KEY (maBH),
        CONSTRAINT FK_PhieuBaoHanh_SerialMay FOREIGN KEY (maSerial) REFERENCES SerialMay(maSerial),
        CONSTRAINT FK_PhieuBaoHanh_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_PhieuBaoHanh_TrangThai CHECK (
            trangThai IN (N'TIEP_NHAN', N'DANG_XU_LY', N'HOAN_TAT')
        ),
        CONSTRAINT CK_PhieuBaoHanh_ChiPhi CHECK (chiPhi >= 0)
    );
END;
GO

-- =========================================================
-- 9. BIẾN ĐỘNG KHO VÀ LỊCH SỬ TÁCH MÃ
-- =========================================================

IF OBJECT_ID(N'BienDongKho', N'U') IS NULL
BEGIN
    CREATE TABLE BienDongKho (
        maBD BIGINT IDENTITY(1,1) NOT NULL,
        loaiBienDong NVARCHAR(20) NOT NULL,
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

        CONSTRAINT CK_BienDongKho_loaiBienDong CHECK (
            loaiBienDong IN (N'NHAP', N'BAN', N'THU_CU', N'HOAN_HUY')
        ),
        CONSTRAINT CK_BienDongKho_soLuong CHECK (soLuong > 0),
        CONSTRAINT CK_BienDongKho_soLuongTruoc CHECK (soLuongTruoc >= 0),
        CONSTRAINT CK_BienDongKho_soLuongSau CHECK (
            soLuongSau >= 0
            AND (
                (loaiBienDong = N'BAN' AND soLuongSau = soLuongTruoc - soLuong)
                OR (loaiBienDong <> N'BAN' AND soLuongSau = soLuongTruoc + soLuong)
            )
        ),
        CONSTRAINT CK_BienDongKho_chungTu CHECK (
            CASE WHEN maCTPN IS NOT NULL THEN 1 ELSE 0 END
            + CASE WHEN maCTHD IS NOT NULL THEN 1 ELSE 0 END
            + CASE WHEN maCTTC IS NOT NULL THEN 1 ELSE 0 END
            = 1
        ),
        CONSTRAINT CK_BienDongKho_loaiVaChungTu CHECK (
            (loaiBienDong = N'NHAP' AND maCTPN IS NOT NULL)
            OR (loaiBienDong = N'THU_CU' AND maCTTC IS NOT NULL)
            OR (loaiBienDong IN (N'BAN', N'HOAN_HUY') AND maCTHD IS NOT NULL)
        ),

        CONSTRAINT FK_BienDongKho_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho),
        CONSTRAINT FK_BienDongKho_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
        CONSTRAINT FK_BienDongKho_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_BienDongKho_ChiTietPhieuNhap FOREIGN KEY (maCTPN) REFERENCES ChiTietPhieuNhap(maCTPN),
        CONSTRAINT FK_BienDongKho_ChiTietHoaDon FOREIGN KEY (maCTHD) REFERENCES ChiTietHoaDon(maCTHD),
        CONSTRAINT FK_BienDongKho_ChiTietPhieuThuCu FOREIGN KEY (maCTTC) REFERENCES ChiTietPhieuThuCu(maCTTC)
    );
END;
GO

IF OBJECT_ID(N'LichSuTachMaSanPham', N'U') IS NULL
BEGIN
    CREATE TABLE LichSuTachMaSanPham (
        maTach BIGINT IDENTITY(1,1) NOT NULL,
        maSPGoc BIGINT NOT NULL,
        maSPMoi BIGINT NOT NULL,
        maKho BIGINT NOT NULL,
        soLuong INT NOT NULL,
        ngayTach DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

        CONSTRAINT PK_LichSuTachMaSanPham PRIMARY KEY (maTach),
        CONSTRAINT UQ_LichSuTachMa UNIQUE (maSPGoc, maSPMoi, maKho),
        CONSTRAINT CK_LichSuTachMa_soLuong CHECK (soLuong > 0),
        CONSTRAINT FK_LichSuTachMa_SanPhamGoc FOREIGN KEY (maSPGoc) REFERENCES SanPham(maSP),
        CONSTRAINT FK_LichSuTachMa_SanPhamMoi FOREIGN KEY (maSPMoi) REFERENCES SanPham(maSP),
        CONSTRAINT FK_LichSuTachMa_Kho FOREIGN KEY (maKho) REFERENCES Kho(maKho)
    );
END;
GO

-- =========================================================
-- 10. GIỎ HÀNG, LỊCH SỬ TRẠNG THÁI, ĐÁNH GIÁ
-- =========================================================

IF OBJECT_ID(N'GioHang', N'U') IS NULL
BEGIN
    CREATE TABLE GioHang (
        maGioHang BIGINT IDENTITY(1,1) NOT NULL,
        maKH BIGINT NULL,
        maKhach VARCHAR(36) NULL,
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ngayCapNhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT PK_GioHang PRIMARY KEY (maGioHang),
        CONSTRAINT FK_GioHang_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
        CONSTRAINT CK_GioHang_chuSoHuu CHECK (
            (maKH IS NOT NULL AND maKhach IS NULL)
            OR (maKH IS NULL AND maKhach IS NOT NULL)
        )
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_GioHang_maKH'
               AND object_id = OBJECT_ID(N'GioHang'))
    CREATE UNIQUE INDEX UX_GioHang_maKH ON GioHang(maKH) WHERE maKH IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_GioHang_maKhach'
               AND object_id = OBJECT_ID(N'GioHang'))
    CREATE UNIQUE INDEX UX_GioHang_maKhach ON GioHang(maKhach) WHERE maKhach IS NOT NULL;
GO

IF OBJECT_ID(N'ChiTietGioHang', N'U') IS NULL
BEGIN
    -- Cố ý không có FK tới SanPham: giỏ đã xử lý sản phẩm ngừng kinh doanh
    CREATE TABLE ChiTietGioHang (
        maGioHang BIGINT NOT NULL,
        maSP BIGINT NOT NULL,
        soLuong INT NOT NULL,
        thuTu INT NOT NULL,

        CONSTRAINT PK_ChiTietGioHang PRIMARY KEY (maGioHang, maSP),
        CONSTRAINT FK_ChiTietGioHang_GioHang FOREIGN KEY (maGioHang)
            REFERENCES GioHang(maGioHang) ON DELETE CASCADE,
        CONSTRAINT CK_ChiTietGioHang_soLuong CHECK (soLuong > 0)
    );
END;
GO

IF OBJECT_ID(N'LichSuTrangThaiHoaDon', N'U') IS NULL
BEGIN
    CREATE TABLE LichSuTrangThaiHoaDon (
        maLS BIGINT IDENTITY(1,1) NOT NULL,
        maHD BIGINT NOT NULL,
        trangThaiCu NVARCHAR(20) NULL,
        trangThaiMoi NVARCHAR(20) NOT NULL,
        maNV BIGINT NULL,
        thoiGian DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT PK_LichSuTrangThaiHoaDon PRIMARY KEY (maLS),
        CONSTRAINT FK_LSTT_HoaDon FOREIGN KEY (maHD) REFERENCES HoaDon(maHD),
        CONSTRAINT FK_LSTT_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV)
    );
END;
GO

IF OBJECT_ID(N'DanhGia', N'U') IS NULL
BEGIN
    CREATE TABLE DanhGia (
        maDG BIGINT IDENTITY(1,1) NOT NULL,
        maSP BIGINT NOT NULL,
        maKH BIGINT NOT NULL,
        soSao INT NOT NULL,
        noiDung NVARCHAR(1000) NOT NULL,
        ngayTao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT PK_DanhGia PRIMARY KEY (maDG),
        CONSTRAINT FK_DanhGia_SanPham FOREIGN KEY (maSP) REFERENCES SanPham(maSP),
        CONSTRAINT FK_DanhGia_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
        CONSTRAINT CK_DanhGia_soSao CHECK (soSao BETWEEN 1 AND 5),
        CONSTRAINT CK_DanhGia_noiDung CHECK (LEN(LTRIM(RTRIM(noiDung))) >= 10),
        CONSTRAINT UQ_DanhGia_SP_KH UNIQUE (maSP, maKH)
    );
END;
GO

-- =========================================================
-- 11. ĐỒNG BỘ CẤU TRÚC CHO DB ĐÃ TẠO TỪ PHIÊN BẢN CŨ
--     (batch 1: thêm cột, batch 2: thêm constraint/index)
-- =========================================================

IF COL_LENGTH(N'HoaDon', N'tenNguoiNhan') IS NULL
    ALTER TABLE HoaDon ADD tenNguoiNhan NVARCHAR(100) NULL;
IF COL_LENGTH(N'HoaDon', N'soDienThoaiNhan') IS NULL
    ALTER TABLE HoaDon ADD soDienThoaiNhan NVARCHAR(10) NULL;
IF COL_LENGTH(N'HoaDon', N'diaChi') IS NULL
    ALTER TABLE HoaDon ADD diaChi NVARCHAR(255) NULL;
IF COL_LENGTH(N'HoaDon', N'ghiChu') IS NULL
    ALTER TABLE HoaDon ADD ghiChu NVARCHAR(255) NULL;
IF COL_LENGTH(N'HoaDon', N'maYeuCau') IS NULL
    ALTER TABLE HoaDon ADD maYeuCau VARCHAR(36) NULL;
IF COL_LENGTH(N'HoaDon', N'phiVanChuyen') IS NULL
    ALTER TABLE HoaDon ADD phiVanChuyen DECIMAL(18,0) NOT NULL
        CONSTRAINT DF_HoaDon_phiVanChuyen DEFAULT 0;
IF COL_LENGTH(N'HoaDon', N'giamGia') IS NULL
    ALTER TABLE HoaDon ADD giamGia DECIMAL(18,0) NOT NULL
        CONSTRAINT DF_HoaDon_giamGia DEFAULT 0;
IF COL_LENGTH(N'HoaDon', N'maGiamGia') IS NULL
    ALTER TABLE HoaDon ADD maGiamGia VARCHAR(30) NULL;

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'HoaDon') AND name = N'maKho' AND is_nullable = 0
)
    ALTER TABLE HoaDon ALTER COLUMN maKho BIGINT NULL;

IF COL_LENGTH(N'ChiTietHoaDon', N'maKho') IS NULL
    ALTER TABLE ChiTietHoaDon ADD maKho BIGINT NULL;

IF COL_LENGTH(N'SerialMay', N'maKho') IS NULL
    ALTER TABLE SerialMay ADD maKho BIGINT NULL;

IF COL_LENGTH(N'ThanhToan', N'maGiaoDich') IS NULL
    ALTER TABLE ThanhToan ADD maGiaoDich NVARCHAR(50) NULL;
IF COL_LENGTH(N'ThanhToan', N'maNVDuyet') IS NULL
    ALTER TABLE ThanhToan ADD maNVDuyet BIGINT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = N'CK_HoaDon_phiVanChuyen'
                 AND parent_object_id = OBJECT_ID(N'dbo.HoaDon'))
    ALTER TABLE dbo.HoaDon ADD CONSTRAINT CK_HoaDon_phiVanChuyen CHECK (phiVanChuyen >= 0);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = N'CK_HoaDon_giamGia'
                 AND parent_object_id = OBJECT_ID(N'dbo.HoaDon'))
    ALTER TABLE dbo.HoaDon ADD CONSTRAINT CK_HoaDon_giamGia CHECK (giamGia >= 0);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE name = N'FK_HoaDon_MaGiamGia'
                 AND parent_object_id = OBJECT_ID(N'dbo.HoaDon'))
    ALTER TABLE dbo.HoaDon ADD CONSTRAINT FK_HoaDon_MaGiamGia
        FOREIGN KEY (maGiamGia) REFERENCES dbo.MaGiamGia(maCode);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE name = N'FK_ChiTietHoaDon_Kho'
                 AND parent_object_id = OBJECT_ID(N'dbo.ChiTietHoaDon'))
    ALTER TABLE dbo.ChiTietHoaDon ADD CONSTRAINT FK_ChiTietHoaDon_Kho
        FOREIGN KEY (maKho) REFERENCES dbo.Kho(maKho);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE name = N'FK_SerialMay_Kho'
                 AND parent_object_id = OBJECT_ID(N'dbo.SerialMay'))
    ALTER TABLE dbo.SerialMay ADD CONSTRAINT FK_SerialMay_Kho
        FOREIGN KEY (maKho) REFERENCES dbo.Kho(maKho);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE name = N'FK_ThanhToan_NVDuyet'
                 AND parent_object_id = OBJECT_ID(N'dbo.ThanhToan'))
    ALTER TABLE dbo.ThanhToan ADD CONSTRAINT FK_ThanhToan_NVDuyet
        FOREIGN KEY (maNVDuyet) REFERENCES dbo.NhanVien(maNV);
GO

-- =========================================================
-- 12. INDEX
-- =========================================================

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'TaiKhoan') AND name = N'UQ_TaiKhoan_maNV')
    CREATE UNIQUE INDEX UQ_TaiKhoan_maNV ON TaiKhoan(maNV) WHERE maNV IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'TaiKhoan') AND name = N'UQ_TaiKhoan_maKH')
    CREATE UNIQUE INDEX UQ_TaiKhoan_maKH ON TaiKhoan(maKH) WHERE maKH IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'SanPham') AND name = N'UQ_SanPham_Goc_TinhTrang')
    CREATE UNIQUE INDEX UQ_SanPham_Goc_TinhTrang
        ON SanPham(maSPGoc, tinhTrang) WHERE maSPGoc IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'HoaDon') AND name = N'UQ_HoaDon_maYeuCau')
    CREATE UNIQUE INDEX UQ_HoaDon_maYeuCau ON HoaDon(maYeuCau) WHERE maYeuCau IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'ThanhToan') AND name = N'UQ_ThanhToan_maHD')
    CREATE UNIQUE INDEX UQ_ThanhToan_maHD ON ThanhToan(maHD);

-- Một mã giao dịch ngân hàng chỉ dùng được cho một khoản thanh toán
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'ThanhToan') AND name = N'UX_ThanhToan_maGiaoDich')
    CREATE UNIQUE INDEX UX_ThanhToan_maGiaoDich
        ON ThanhToan(maGiaoDich) WHERE maGiaoDich IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'BienDongKho') AND name = N'IX_BienDongKho_maSP_thoiGian')
    CREATE INDEX IX_BienDongKho_maSP_thoiGian ON BienDongKho(maSP, thoiGian);
GO

-- =========================================================
-- 13. TRIGGER KIỂM TRA SẢN PHẨM MỚI/CŨ
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_SanPham_KiemTraGiaCu
ON SanPham
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN LoaiSanPham l ON l.maLoai = i.maLoai
        WHERE l.tenLoai = N'Đĩa game'
          AND i.tinhTrang <> N'MOI'
    )
    BEGIN
        ;THROW 50030, N'Đĩa game chỉ được khai báo là hàng mới.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM SanPham cu WITH (UPDLOCK, HOLDLOCK)
        JOIN SanPham moi WITH (UPDLOCK, HOLDLOCK)
            ON moi.maSP = cu.maSPGoc
        WHERE (
            cu.maSP IN (SELECT maSP FROM inserted)
            OR moi.maSP IN (SELECT maSP FROM inserted)
        )
        AND (
            cu.tinhTrang <> N'CU'
            OR moi.tinhTrang <> N'MOI'
            OR cu.giaBan >= moi.giaBan
            OR cu.maLoai <> moi.maLoai
            OR ISNULL(cu.maNenTang, -1) <> ISNULL(moi.maNenTang, -1)
        )
    )
    BEGIN
        ;THROW 50031,
            N'Mã hàng cũ phải cùng loại, cùng nền tảng và có giá thấp hơn mã hàng mới gốc.',
            1;
    END;
END;
GO

-- =========================================================
-- 14. TRIGGER BÁN HÀNG
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_ChiTietHoaDon_After_Insert
ON ChiTietHoaDon
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE
        @id BIGINT,
        @hd BIGINT,
        @sp BIGINT,
        @serial BIGINT,
        @lineWarehouse BIGINT,
        @warehouse BIGINT,
        @employee BIGINT,
        @quantity INT,
        @price DECIMAL(18,0),
        @before INT,
        @status NVARCHAR(20);

    DECLARE detail_rows CURSOR LOCAL FAST_FORWARD FOR
        SELECT maCTHD, maHD, maSP, maSerial, maKho, soLuong, donGia
        FROM inserted
        ORDER BY maSP, maKho, maCTHD;

    OPEN detail_rows;

    FETCH NEXT FROM detail_rows
    INTO @id, @hd, @sp, @serial, @lineWarehouse, @quantity, @price;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @warehouse = NULL;
        SET @status = NULL;

        SELECT
            @warehouse = COALESCE(@lineWarehouse, maKho),
            @employee = maNV,
            @status = trangThai
        FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
        WHERE maHD = @hd;

        IF @warehouse IS NULL OR @status IS NULL
        BEGIN
            ;THROW 54002, N'Chưa xác định được hóa đơn hoặc kho xuất.', 1;
        END;

        IF @status = N'HUY'
        BEGIN
            ;THROW 54003, N'Không thể thêm sản phẩm vào hóa đơn đã hủy.', 1;
        END;

        IF @serial IS NOT NULL
        BEGIN
            IF @quantity <> 1
            BEGIN
                ;THROW 54004, N'Dòng có serial phải có số lượng bằng 1.', 1;
            END;

            IF NOT EXISTS (
                SELECT 1
                FROM SerialMay WITH (UPDLOCK, HOLDLOCK)
                WHERE maSerial = @serial
                  AND maSP = @sp
                  AND maKho = @warehouse
                  AND trangThai = N'TRONG_KHO'
            )
            BEGIN
                ;THROW 54005, N'Serial không hợp lệ hoặc không thuộc kho xuất.', 1;
            END;
        END;

        SET @before = NULL;

        SELECT @before = soLuong
        FROM TonKho WITH (UPDLOCK, HOLDLOCK)
        WHERE maKho = @warehouse
          AND maSP = @sp;

        IF @before IS NULL OR @before < @quantity
        BEGIN
            ;THROW 54006, N'Không đủ tồn kho để đặt hàng.', 1;
        END;

        UPDATE TonKho
        SET soLuong = soLuong - @quantity
        WHERE maKho = @warehouse
          AND maSP = @sp;

        INSERT INTO BienDongKho (
            loaiBienDong, soLuong, soLuongTruoc, soLuongSau,
            maKho, maSP, maNV, maCTHD
        )
        VALUES (
            N'BAN', @quantity, @before, @before - @quantity,
            @warehouse, @sp, @employee, @id
        );

        IF @serial IS NOT NULL
        BEGIN
            UPDATE SerialMay
            SET trangThai = N'DA_BAN'
            WHERE maSerial = @serial;
        END;

        UPDATE HoaDon
        SET tongTien = tongTien + @quantity * @price
        WHERE maHD = @hd;

        FETCH NEXT FROM detail_rows
        INTO @id, @hd, @sp, @serial, @lineWarehouse, @quantity, @price;
    END;

    CLOSE detail_rows;
    DEALLOCATE detail_rows;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_HoaDon_Before_Update
ON HoaDon
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        WHERE (d.trangThai = N'HUY' AND i.trangThai <> N'HUY')
           OR (d.trangThai = N'HOAN_TAT' AND i.trangThai <> N'HOAN_TAT')
    )
    BEGIN
        ;THROW 54009, N'Không thể mở lại hoặc sửa trạng thái đơn đã kết thúc.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        WHERE ISNULL(i.maKho, -1) <> ISNULL(d.maKho, -1)
          AND EXISTS (SELECT 1 FROM ChiTietHoaDon c WHERE c.maHD = i.maHD)
    )
    BEGIN
        ;THROW 54010, N'Không đổi kho của hóa đơn đã có chi tiết.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        WHERE i.trangThai = N'HUY'
          AND d.trangThai <> N'HUY'
          AND (
              d.trangThai = N'DANG_GIAO'
              OR EXISTS (
                  SELECT 1
                  FROM ThanhToan t
                  WHERE t.maHD = i.maHD
                    AND t.trangThai = N'DA_THANH_TOAN'
              )
          )
    )
    BEGIN
        ;THROW 54011, N'Đơn đã thanh toán hoặc đang giao cần nghiệp vụ hoàn trả riêng.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        WHERE i.trangThai <> d.trangThai
          AND i.trangThai IN (N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT')
          AND NOT EXISTS (
              SELECT 1
              FROM ThanhToan t
              WHERE t.maHD = i.maHD
                AND t.soTien = i.tongTien
                AND (
                    t.trangThai = N'DA_THANH_TOAN'
                    OR (
                        t.phuongThuc = N'TIEN_MAT'
                        AND i.trangThai IN (N'DA_XAC_NHAN', N'DANG_GIAO')
                    )
                )
          )
    )
    BEGIN
        ;THROW 54012, N'Chưa xác nhận đủ thanh toán cho hóa đơn.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        JOIN ChiTietHoaDon c ON c.maHD = i.maHD
        JOIN SanPham p ON p.maSP = c.maSP
        JOIN LoaiSanPham l ON l.maLoai = p.maLoai
        WHERE i.trangThai <> d.trangThai
          AND i.trangThai IN (N'DANG_GIAO', N'HOAN_TAT')
          AND l.tenLoai = N'Máy console'
          AND c.maSerial IS NULL
    )
    BEGIN
        ;THROW 54013, N'Cần gán đủ serial máy trước khi giao hàng.', 1;
    END;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ThanhToan_KiemTra
ON ThanhToan
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted
        WHERE phuongThuc NOT IN (N'TIEN_MAT', N'CHUYEN_KHOAN', N'MOMO', N'ZALOPAY')
           OR (trangThai = N'CHUA_THANH_TOAN' AND ngayThanhToan IS NOT NULL)
           OR (trangThai = N'DA_THANH_TOAN' AND ngayThanhToan IS NULL)
    )
    BEGIN
        ;THROW 54019, N'Thông tin thanh toán không hợp lệ.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maTT = i.maTT
        WHERE i.maHD <> d.maHD
    )
    BEGIN
        ;THROW 56001, N'Không chuyển giao dịch thanh toán sang hóa đơn khác.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN HoaDon h WITH (UPDLOCK, HOLDLOCK) ON h.maHD = i.maHD
        WHERE i.soTien <> h.tongTien
           OR h.tongTien <= 0
           OR (i.trangThai = N'DA_THANH_TOAN' AND h.trangThai = N'HUY')
    )
    BEGIN
        ;THROW 54020, N'Số tiền hoặc trạng thái hóa đơn không phù hợp.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN HoaDon h ON h.maHD = i.maHD
        WHERE h.kenhBan = N'QUAY'
          AND i.phuongThuc = N'TIEN_MAT'
          AND i.trangThai <> N'DA_THANH_TOAN'
    )
    BEGIN
        ;THROW 56003, N'Chỉ ghi nhận thanh toán tiền mặt sau khi đã nhận tiền.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maTT = i.maTT
        WHERE d.trangThai = N'DA_THANH_TOAN'
          AND (
              i.trangThai <> d.trangThai
              OR i.soTien <> d.soTien
              OR i.phuongThuc <> d.phuongThuc
              OR ISNULL(i.ngayThanhToan, '19000101') <> ISNULL(d.ngayThanhToan, '19000101')
          )
    )
    BEGIN
        ;THROW 54021, N'Không sửa giao dịch đã xác nhận thanh toán.', 1;
    END;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_HoaDon_After_Update
ON HoaDon
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE
        @detailId BIGINT,
        @productId BIGINT,
        @quantity INT,
        @serialId BIGINT,
        @warehouseId BIGINT,
        @employeeId BIGINT,
        @before INT;

    DECLARE cancelled_rows CURSOR LOCAL FAST_FORWARD FOR
        SELECT
            c.maCTHD,
            c.maSP,
            c.soLuong,
            c.maSerial,
            COALESCE(c.maKho, i.maKho),
            i.maNV
        FROM inserted i
        JOIN deleted d ON d.maHD = i.maHD
        JOIN ChiTietHoaDon c ON c.maHD = i.maHD
        WHERE d.trangThai <> N'HUY'
          AND i.trangThai = N'HUY'
        ORDER BY c.maSP, c.maKho, c.maCTHD;

    OPEN cancelled_rows;

    FETCH NEXT FROM cancelled_rows
    INTO @detailId, @productId, @quantity, @serialId, @warehouseId, @employeeId;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF @warehouseId IS NULL
        BEGIN
            ;THROW 54007, N'Không xác định được kho cần hoàn tồn.', 1;
        END;

        SET @before = NULL;

        SELECT @before = soLuong
        FROM TonKho WITH (UPDLOCK, HOLDLOCK)
        WHERE maKho = @warehouseId
          AND maSP = @productId;

        IF @before IS NULL
        BEGIN
            INSERT INTO TonKho(maKho, maSP, soLuong)
            VALUES (@warehouseId, @productId, 0);

            SET @before = 0;
        END;

        IF @before > 2147483647 - @quantity
        BEGIN
            ;THROW 54008, N'Tồn kho vượt giới hạn INT.', 1;
        END;

        UPDATE TonKho
        SET soLuong = soLuong + @quantity
        WHERE maKho = @warehouseId
          AND maSP = @productId;

        INSERT INTO BienDongKho (
            loaiBienDong, soLuong, soLuongTruoc, soLuongSau,
            maKho, maSP, maNV, maCTHD
        )
        VALUES (
            N'HOAN_HUY', @quantity, @before, @before + @quantity,
            @warehouseId, @productId, @employeeId, @detailId
        );

        IF @serialId IS NOT NULL
        BEGIN
            UPDATE SerialMay
            SET trangThai = N'TRONG_KHO'
            WHERE maSerial = @serialId;
        END;

        FETCH NEXT FROM cancelled_rows
        INTO @detailId, @productId, @quantity, @serialId, @warehouseId, @employeeId;
    END;

    CLOSE cancelled_rows;
    DEALLOCATE cancelled_rows;
END;
GO

-- =========================================================
-- 15. TRIGGER THU CŨ VÀ NHẬP HÀNG
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuThuCu_After_Insert
ON ChiTietPhieuThuCu
AFTER INSERT
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN SanPham p ON p.maSP = i.maSP
        JOIN LoaiSanPham l ON l.maLoai = p.maLoai
        WHERE p.tinhTrang <> N'CU'
           OR l.tenLoai = N'Đĩa game'
    )
    BEGIN
        ;THROW 50021, N'Chỉ thu cũ thiết bị có mã hàng cũ; không thu đĩa game.', 1;
    END;

    DECLARE
        @id BIGINT,
        @parent BIGINT,
        @sp BIGINT,
        @qty INT,
        @price DECIMAL(18,0),
        @kho BIGINT,
        @nv BIGINT,
        @before INT;

    DECLARE rows_to_process CURSOR LOCAL FAST_FORWARD FOR
        SELECT maCTTC, maPTC, maSP, soLuong, donGiaThu
        FROM inserted
        ORDER BY maCTTC;

    OPEN rows_to_process;

    FETCH NEXT FROM rows_to_process INTO @id, @parent, @sp, @qty, @price;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SELECT @kho = maKho, @nv = maNV
        FROM PhieuThuCu WITH (UPDLOCK, HOLDLOCK)
        WHERE maPTC = @parent;

        SET @before = NULL;

        SELECT @before = soLuong
        FROM TonKho WITH (UPDLOCK, HOLDLOCK)
        WHERE maKho = @kho
          AND maSP = @sp;

        IF @before IS NULL
        BEGIN
            INSERT INTO TonKho(maKho, maSP, soLuong)
            VALUES (@kho, @sp, 0);

            SET @before = 0;
        END;

        IF @before > 2147483647 - @qty
        BEGIN
            ;THROW 50007, N'Tồn kho vượt giới hạn INT.', 1;
        END;

        UPDATE TonKho
        SET soLuong = soLuong + @qty
        WHERE maKho = @kho
          AND maSP = @sp;

        UPDATE PhieuThuCu
        SET tongTienThu = tongTienThu + @qty * @price
        WHERE maPTC = @parent;

        INSERT INTO BienDongKho (
            loaiBienDong, soLuong, soLuongTruoc, soLuongSau,
            maKho, maSP, maNV, maCTTC
        )
        VALUES (
            N'THU_CU', @qty, @before, @before + @qty,
            @kho, @sp, @nv, @id
        );

        FETCH NEXT FROM rows_to_process INTO @id, @parent, @sp, @qty, @price;
    END;

    CLOSE rows_to_process;
    DEALLOCATE rows_to_process;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuNhap_After_Insert
ON ChiTietPhieuNhap
AFTER INSERT
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    DECLARE
        @id BIGINT,
        @parent BIGINT,
        @sp BIGINT,
        @qty INT,
        @price DECIMAL(18,0),
        @kho BIGINT,
        @nv BIGINT,
        @before INT;

    DECLARE rows_to_process CURSOR LOCAL FAST_FORWARD FOR
        SELECT maCTPN, maPN, maSP, soLuong, donGiaNhap
        FROM inserted
        ORDER BY maCTPN;

    OPEN rows_to_process;

    FETCH NEXT FROM rows_to_process INTO @id, @parent, @sp, @qty, @price;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SELECT @kho = maKho, @nv = maNV
        FROM PhieuNhap WITH (UPDLOCK, HOLDLOCK)
        WHERE maPN = @parent;

        SET @before = NULL;

        SELECT @before = soLuong
        FROM TonKho WITH (UPDLOCK, HOLDLOCK)
        WHERE maKho = @kho
          AND maSP = @sp;

        IF @before IS NULL
        BEGIN
            INSERT INTO TonKho(maKho, maSP, soLuong)
            VALUES (@kho, @sp, 0);

            SET @before = 0;
        END;

        IF @before > 2147483647 - @qty
        BEGIN
            ;THROW 50007, N'Tồn kho vượt giới hạn INT.', 1;
        END;

        UPDATE TonKho
        SET soLuong = soLuong + @qty
        WHERE maKho = @kho
          AND maSP = @sp;

        UPDATE PhieuNhap
        SET tongTien = tongTien + @qty * @price
        WHERE maPN = @parent;

        INSERT INTO BienDongKho (
            loaiBienDong, soLuong, soLuongTruoc, soLuongSau,
            maKho, maSP, maNV, maCTPN
        )
        VALUES (
            N'NHAP', @qty, @before, @before + @qty,
            @kho, @sp, @nv, @id
        );

        FETCH NEXT FROM rows_to_process INTO @id, @parent, @sp, @qty, @price;
    END;

    CLOSE rows_to_process;
    DEALLOCATE rows_to_process;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_CTPNSerial_Before_Insert
ON ChiTietPhieuNhapSerial
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN ChiTietPhieuNhap c WITH (UPDLOCK, HOLDLOCK) ON c.maCTPN = i.maCTPN
        JOIN PhieuNhap p ON p.maPN = c.maPN
        JOIN SerialMay s WITH (UPDLOCK, HOLDLOCK) ON s.maSerial = i.maSerial
        WHERE c.maSP <> s.maSP
           OR (s.maKho IS NOT NULL AND s.maKho <> p.maKho)
    )
    BEGIN
        ;THROW 54017, N'Serial không phù hợp với sản phẩm hoặc kho nhập.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM ChiTietPhieuNhap c WITH (UPDLOCK, HOLDLOCK)
        WHERE c.maCTPN IN (SELECT maCTPN FROM inserted)
          AND (
              SELECT COUNT_BIG(*)
              FROM ChiTietPhieuNhapSerial x WITH (UPDLOCK, HOLDLOCK)
              WHERE x.maCTPN = c.maCTPN
          ) > c.soLuong
    )
    BEGIN
        ;THROW 54018, N'Số serial vượt số lượng nhập.', 1;
    END;

    UPDATE s
    SET s.maKho = p.maKho
    FROM SerialMay s
    JOIN inserted i ON i.maSerial = s.maSerial
    JOIN ChiTietPhieuNhap c ON c.maCTPN = i.maCTPN
    JOIN PhieuNhap p ON p.maPN = c.maPN;
END;
GO

-- =========================================================
-- 16. TRIGGER BẢO HÀNH
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_PhieuBaoHanh_Before_Insert
ON PhieuBaoHanh
AFTER INSERT
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    DECLARE
        @id BIGINT,
        @serial BIGINT,
        @received DATETIME,
        @status NVARCHAR(20),
        @sold DATETIME;

    DECLARE rows_to_process CURSOR LOCAL FAST_FORWARD FOR
        SELECT maBH, maSerial, ngayTiepNhan, trangThai
        FROM inserted
        ORDER BY maBH;

    OPEN rows_to_process;

    FETCH NEXT FROM rows_to_process INTO @id, @serial, @received, @status;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM SerialMay WITH (UPDLOCK, HOLDLOCK)
            WHERE maSerial = @serial
              AND trangThai = N'DA_BAN'
        )
        BEGIN
            ;THROW 50010, N'Serial chưa bán hoặc đang bảo hành, không thể tiếp nhận.', 1;
        END;

        SET @sold = NULL;

        SELECT @sold = MIN(hd.ngayLap)
        FROM ChiTietHoaDon ct
        JOIN HoaDon hd ON hd.maHD = ct.maHD
        WHERE ct.maSerial = @serial
          AND hd.trangThai <> N'HUY';

        UPDATE PhieuBaoHanh
        SET trangThaiBaoHanh =
            CASE
                WHEN @sold IS NOT NULL
                 AND @received < DATEADD(MONTH, 12, @sold)
                THEN 1
                ELSE 0
            END
        WHERE maBH = @id;

        IF @status <> N'HOAN_TAT'
        BEGIN
            UPDATE SerialMay
            SET trangThai = N'BAO_HANH'
            WHERE maSerial = @serial;
        END;

        FETCH NEXT FROM rows_to_process INTO @id, @serial, @received, @status;
    END;

    CLOSE rows_to_process;
    DEALLOCATE rows_to_process;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_PhieuBaoHanh_After_Update
ON PhieuBaoHanh
AFTER UPDATE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    UPDATE s
    SET trangThai = N'DA_BAN'
    FROM SerialMay s
    JOIN inserted i ON i.maSerial = s.maSerial
    JOIN deleted d ON d.maBH = i.maBH
    WHERE d.trangThai <> N'HOAN_TAT'
      AND i.trangThai = N'HOAN_TAT';
END;
GO

-- =========================================================
-- 17. KIỂM SOÁT SỬA/XÓA CHI TIẾT CHỨNG TỪ
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_ChiTietHoaDon_Before_Update
ON ChiTietHoaDon
INSTEAD OF UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.maCTHD = i.maCTHD
        WHERE i.maHD <> d.maHD
           OR i.maSP <> d.maSP
           OR i.soLuong <> d.soLuong
           OR i.donGia <> d.donGia
           OR ISNULL(i.maKho, -1) <> ISNULL(d.maKho, -1)
           OR (d.maSerial IS NOT NULL AND ISNULL(i.maSerial, -1) <> d.maSerial)
    )
    BEGIN
        ;THROW 54014, N'Chỉ được bổ sung serial cho chi tiết hóa đơn.', 1;
    END;

    DECLARE
        @detailId BIGINT,
        @invoiceId BIGINT,
        @productId BIGINT,
        @serialId BIGINT,
        @warehouseId BIGINT,
        @quantity INT,
        @status NVARCHAR(20);

    DECLARE serial_rows CURSOR LOCAL FAST_FORWARD FOR
        SELECT i.maCTHD, i.maHD, i.maSP, i.maSerial, i.maKho, i.soLuong
        FROM inserted i
        JOIN deleted d ON d.maCTHD = i.maCTHD
        WHERE d.maSerial IS NULL
          AND i.maSerial IS NOT NULL
        ORDER BY i.maHD, i.maSerial;

    OPEN serial_rows;

    FETCH NEXT FROM serial_rows
    INTO @detailId, @invoiceId, @productId, @serialId, @warehouseId, @quantity;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SELECT
            @status = trangThai,
            @warehouseId = COALESCE(@warehouseId, maKho)
        FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
        WHERE maHD = @invoiceId;

        IF @status <> N'DA_XAC_NHAN' OR @quantity <> 1
        BEGIN
            ;THROW 54015, N'Chỉ gán serial cho đơn đã xác nhận, số lượng 1.', 1;
        END;

        IF NOT EXISTS (
            SELECT 1
            FROM SerialMay WITH (UPDLOCK, HOLDLOCK)
            WHERE maSerial = @serialId
              AND maSP = @productId
              AND maKho = @warehouseId
              AND trangThai = N'TRONG_KHO'
        )
        BEGIN
            ;THROW 54016, N'Serial không thuộc sản phẩm, kho hoặc đã được sử dụng.', 1;
        END;

        UPDATE SerialMay
        SET trangThai = N'DA_BAN'
        WHERE maSerial = @serialId;

        UPDATE ChiTietHoaDon
        SET maSerial = @serialId
        WHERE maCTHD = @detailId;

        FETCH NEXT FROM serial_rows
        INTO @detailId, @invoiceId, @productId, @serialId, @warehouseId, @quantity;
    END;

    CLOSE serial_rows;
    DEALLOCATE serial_rows;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietHoaDon_Before_Delete
ON ChiTietHoaDon
INSTEAD OF DELETE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    ;THROW 50011, N'Không được xóa chi tiết hóa đơn, hãy hủy hóa đơn.', 1;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuNhap_Before_Update
ON ChiTietPhieuNhap
INSTEAD OF UPDATE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    ;THROW 50011, N'Không được sửa chi tiết phiếu nhập.', 1;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuNhap_Before_Delete
ON ChiTietPhieuNhap
INSTEAD OF DELETE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    ;THROW 50011, N'Không được xóa chi tiết phiếu nhập.', 1;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuThuCu_Before_Update
ON ChiTietPhieuThuCu
INSTEAD OF UPDATE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    ;THROW 50011, N'Không được sửa chi tiết phiếu thu cũ.', 1;
END;
GO

CREATE OR ALTER TRIGGER TRIGGER_ChiTietPhieuThuCu_Before_Delete
ON ChiTietPhieuThuCu
INSTEAD OF DELETE
AS
BEGIN
    IF ROWCOUNT_BIG() = 0 RETURN;
    SET NOCOUNT ON;

    ;THROW 50011, N'Không được xóa chi tiết phiếu thu cũ.', 1;
END;
GO

-- =========================================================
-- 18. PROCEDURE HÓA ĐƠN VÀ THANH TOÁN
-- =========================================================

IF OBJECT_ID(N'dbo.sp_TaoHoaDon', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_TaoHoaDon] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_TaoHoaDon
    @p_maKH BIGINT,
    @p_maNV BIGINT,
    @p_maKho BIGINT,
    @p_kenhBan NVARCHAR(20),
    @p_diaChi NVARCHAR(255),
    @p_ghiChu NVARCHAR(255),
    @p_maHD BIGINT OUTPUT,
    @p_tenNguoiNhan NVARCHAR(100) = NULL,
    @p_soDienThoaiNhan NVARCHAR(10) = NULL,
    @p_maYeuCau VARCHAR(36) = NULL,
    @p_phiVanChuyen DECIMAL(18,0) = 0,
    @p_giamGia DECIMAL(18,0) = 0,
    @p_maGiamGia VARCHAR(30) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO HoaDon (
        maKH, maNV, maKho, kenhBan,
        tenNguoiNhan, soDienThoaiNhan,
        diaChi, ghiChu, maYeuCau,
        phiVanChuyen, giamGia, maGiamGia
    )
    VALUES (
        @p_maKH, @p_maNV, @p_maKho, @p_kenhBan,
        @p_tenNguoiNhan, @p_soDienThoaiNhan,
        @p_diaChi, @p_ghiChu, @p_maYeuCau,
        ISNULL(@p_phiVanChuyen, 0), ISNULL(@p_giamGia, 0), @p_maGiamGia
    );

    SET @p_maHD = CAST(SCOPE_IDENTITY() AS BIGINT);
END;
GO

IF OBJECT_ID(N'dbo.sp_ThemChiTietHoaDon', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_ThemChiTietHoaDon] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_ThemChiTietHoaDon
    @p_maHD BIGINT,
    @p_maSP BIGINT,
    @p_maSerial BIGINT,
    @p_soLuong INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @v_gia DECIMAL(18,0) = NULL;

    SELECT @v_gia = giaBan
    FROM SanPham
    WHERE maSP = @p_maSP
      AND deleted = 0;

    IF @v_gia IS NULL
    BEGIN
        ;THROW 50020, N'Sản phẩm không tồn tại hoặc đã ngừng bán.', 1;
    END;

    INSERT INTO ChiTietHoaDon (maHD, maSP, maSerial, soLuong, donGia)
    VALUES (@p_maHD, @p_maSP, @p_maSerial, @p_soLuong, @v_gia);
END;
GO

IF OBJECT_ID(N'dbo.sp_TaoThanhToan', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_TaoThanhToan] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_TaoThanhToan
    @p_maHD BIGINT,
    @p_phuongThuc NVARCHAR(20),
    @p_xacNhanDaNhanTien BIT = 0,
    @p_tienKhachDua DECIMAL(18,0) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @p_phuongThuc IS NULL
       OR @p_phuongThuc NOT IN (N'TIEN_MAT', N'CHUYEN_KHOAN', N'MOMO', N'ZALOPAY')
    BEGIN
        ;THROW 50022, N'Phương thức thanh toán không hợp lệ.', 1;
    END;

    DECLARE @ownTransaction BIT =
        CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;

    DECLARE
        @total DECIMAL(18,0),
        @channel NVARCHAR(20),
        @invoiceStatus NVARCHAR(20),
        @employeeId BIGINT,
        @paymentStatus NVARCHAR(20),
        @paymentDate DATETIME,
        @change DECIMAL(18,0),
        @paymentId BIGINT;

    BEGIN TRY
        IF @ownTransaction = 1
            BEGIN TRANSACTION;
        ELSE
            SAVE TRANSACTION PaymentSavepoint;

        SELECT
            @total = tongTien,
            @channel = kenhBan,
            @invoiceStatus = trangThai,
            @employeeId = maNV
        FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
        WHERE maHD = @p_maHD;

        IF @total IS NULL
        BEGIN
            ;THROW 50020, N'Hóa đơn không tồn tại.', 1;
        END;

        IF @invoiceStatus <> N'CHO_XAC_NHAN'
        BEGIN
            ;THROW 56004, N'Chỉ tạo thanh toán cho hóa đơn đang chờ xác nhận.', 1;
        END;

        IF @total <= 0
        BEGIN
            ;THROW 50024, N'Hóa đơn chưa có sản phẩm để thanh toán.', 1;
        END;

        IF EXISTS (
            SELECT 1
            FROM ThanhToan WITH (UPDLOCK, HOLDLOCK)
            WHERE maHD = @p_maHD
        )
        BEGIN
            ;THROW 50025, N'Hóa đơn đã có thông tin thanh toán.', 1;
        END;

        -- LUỒNG ONLINE
        IF @channel = N'ONLINE'
        BEGIN
            IF ISNULL(@p_xacNhanDaNhanTien, 0) <> 0
               OR @p_tienKhachDua IS NOT NULL
            BEGIN
                ;THROW 56005,
                    N'Đơn online chỉ tạo thanh toán chờ; xác nhận giao dịch qua nghiệp vụ duyệt thanh toán.',
                    1;
            END;

            SET @paymentStatus = N'CHUA_THANH_TOAN';
            SET @paymentDate = NULL;
            SET @change = NULL;
        END

        -- LUỒNG BÁN TẠI QUẦY
        ELSE IF @channel = N'QUAY'
        BEGIN
            IF @employeeId IS NULL
            BEGIN
                ;THROW 56006, N'Hóa đơn tại quầy phải có nhân viên phụ trách.', 1;
            END;

            IF ISNULL(@p_xacNhanDaNhanTien, 0) <> 1
            BEGIN
                ;THROW 56007, N'Nhân viên phải xác nhận đã nhận đủ tiền.', 1;
            END;

            IF @p_phuongThuc = N'TIEN_MAT'
            BEGIN
                IF @p_tienKhachDua IS NULL OR @p_tienKhachDua < @total
                BEGIN
                    ;THROW 56008, N'Tiền khách đưa chưa đủ thanh toán.', 1;
                END;

                SET @change = @p_tienKhachDua - @total;
            END
            ELSE
            BEGIN
                IF @p_tienKhachDua IS NOT NULL
                BEGIN
                    ;THROW 56009,
                        N'Chỉ nhập tiền khách đưa khi chọn thanh toán tiền mặt.',
                        1;
                END;

                SET @change = NULL;
            END;

            SET @paymentStatus = N'DA_THANH_TOAN';
            SET @paymentDate = GETDATE();
        END
        ELSE
        BEGIN
            ;THROW 56010, N'Kênh bán không hợp lệ.', 1;
        END;

        INSERT INTO ThanhToan (maHD, phuongThuc, soTien, trangThai, ngayThanhToan)
        VALUES (@p_maHD, @p_phuongThuc, @total, @paymentStatus, @paymentDate);

        SET @paymentId = CONVERT(BIGINT, SCOPE_IDENTITY());

        IF @channel = N'QUAY'
        BEGIN
            UPDATE HoaDon
            SET trangThai = N'HOAN_TAT'
            WHERE maHD = @p_maHD;
        END;

        IF @ownTransaction = 1
            COMMIT TRANSACTION;

        SELECT
            @paymentId AS maTT,
            @p_maHD AS maHD,
            @channel AS kenhBan,
            @p_phuongThuc AS phuongThuc,
            @total AS soTien,
            @paymentStatus AS trangThai,
            @paymentDate AS ngayThanhToan,
            @p_tienKhachDua AS tienKhachDua,
            @change AS tienThoi;
    END TRY
    BEGIN CATCH
        IF @ownTransaction = 1 AND XACT_STATE() <> 0
        BEGIN
            ROLLBACK TRANSACTION;
        END
        ELSE IF @ownTransaction = 0 AND XACT_STATE() = 1
        BEGIN
            ROLLBACK TRANSACTION PaymentSavepoint;
        END;

        ;THROW;
    END CATCH;
END;
GO

IF OBJECT_ID(N'dbo.sp_CapNhatTrangThaiHoaDon', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_CapNhatTrangThaiHoaDon] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_CapNhatTrangThaiHoaDon
    @p_maHD BIGINT,
    @p_trangThai NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @p_trangThai IS NULL
       OR @p_trangThai NOT IN (
           N'CHO_XAC_NHAN', N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT', N'HUY'
       )
    BEGIN
        ;THROW 50026, N'Trạng thái hóa đơn không hợp lệ.', 1;
    END;

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE
            @v_cu NVARCHAR(20) = NULL,
            @v_tong DECIMAL(18,0);

        SELECT
            @v_cu = trangThai,
            @v_tong = tongTien
        FROM HoaDon WITH (UPDLOCK, HOLDLOCK)
        WHERE maHD = @p_maHD;

        IF @v_cu IS NULL
        BEGIN
            ;THROW 50020, N'Hóa đơn không tồn tại.', 1;
        END;

        IF @v_cu <> @p_trangThai
        BEGIN
            IF @v_cu IN (N'HOAN_TAT', N'HUY')
            BEGIN
                ;THROW 50020, N'Hóa đơn đã kết thúc, không thể đổi trạng thái.', 1;
            END;

            IF @p_trangThai IN (N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT')
            BEGIN
                IF @v_tong <= 0 OR NOT EXISTS (
                    SELECT 1
                    FROM ThanhToan
                    WHERE maHD = @p_maHD
                      AND trangThai = N'DA_THANH_TOAN'
                      AND soTien = @v_tong
                )
                BEGIN
                    ;THROW 50027,
                        N'Phải duyệt thanh toán trước khi xác nhận hoặc hoàn tất hóa đơn.',
                        1;
                END;
            END;

            UPDATE HoaDon
            SET trangThai = @p_trangThai
            WHERE maHD = @p_maHD;
        END;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        ;THROW;
    END CATCH;
END;
GO

-- =========================================================
-- 19. PROCEDURE TRA CỨU VÀ BÁO CÁO
-- =========================================================
IF OBJECT_ID(N'dbo.sp_TiepNhanBaoHanh', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_TiepNhanBaoHanh] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_TiepNhanBaoHanh
    @p_soSerial NVARCHAR(100),
    @p_maNV BIGINT,
    @p_moTaLoi NVARCHAR(255),
    @p_ngayHenTra DATETIME
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @v_maSerial BIGINT;

    SELECT @v_maSerial = maSerial
    FROM SerialMay
    WHERE soSerial = @p_soSerial;

    IF @v_maSerial IS NULL
    BEGIN
        ;THROW 50020, N'Không tìm thấy số serial.', 1;
    END;

    INSERT INTO PhieuBaoHanh (maSerial, maNV, ngayHenTra, moTaLoi)
    VALUES (@v_maSerial, @p_maNV, @p_ngayHenTra, @p_moTaLoi);

    SELECT CAST(SCOPE_IDENTITY() AS BIGINT) AS maBH;
END;
GO

IF OBJECT_ID(N'dbo.sp_TraCuuBaoHanh', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_TraCuuBaoHanh] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_TraCuuBaoHanh
    @p_soSerial NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        s.soSerial,
        sp.tenSP,
        s.trangThai AS trangThaiSerial,
        hd.maHD,
        hd.ngayLap AS ngayBan,
        DATEADD(MONTH, 12, hd.ngayLap) AS hanBaoHanh,
        CAST(
            CASE
                WHEN GETDATE() < DATEADD(MONTH, 12, hd.ngayLap) THEN 1
                ELSE 0
            END AS BIT
        ) AS conHan
    FROM SerialMay s
    JOIN SanPham sp ON sp.maSP = s.maSP
    LEFT JOIN ChiTietHoaDon ct ON ct.maSerial = s.maSerial
    LEFT JOIN HoaDon hd
        ON hd.maHD = ct.maHD
       AND hd.trangThai <> N'HUY'
    WHERE s.soSerial = @p_soSerial;
END;
GO

IF OBJECT_ID(N'dbo.sp_TonKhoTheoKho', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_TonKhoTheoKho] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_TonKhoTheoKho
    @p_maKho BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        k.tenKho,
        sp.maSP,
        sp.tenSP,
        tk.soLuong
    FROM TonKho tk
    JOIN Kho k ON k.maKho = tk.maKho
    JOIN SanPham sp ON sp.maSP = tk.maSP
    WHERE @p_maKho IS NULL OR tk.maKho = @p_maKho
    ORDER BY k.maKho, sp.maSP;
END;
GO

IF OBJECT_ID(N'dbo.sp_BaoCaoDoanhThuTheoThang', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_BaoCaoDoanhThuTheoThang] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_BaoCaoDoanhThuTheoThang
    @p_nam INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        MONTH(ngayLap) AS thang,
        COUNT(*) AS soHoaDon,
        SUM(tongTien) AS doanhThu
    FROM HoaDon
    WHERE trangThai = N'HOAN_TAT'
      AND YEAR(ngayLap) = @p_nam
    GROUP BY MONTH(ngayLap)
    ORDER BY thang;
END;
GO

IF OBJECT_ID(N'dbo.sp_BaoCaoChiPhiNhapTheoNCC', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_BaoCaoChiPhiNhapTheoNCC] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_BaoCaoChiPhiNhapTheoNCC
    @p_tuNgay DATETIME,
    @p_denNgay DATETIME
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        ncc.maNCC,
        ncc.tenNCC,
        SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
    FROM PhieuNhap pn
    JOIN NhaCungCap ncc ON ncc.maNCC = pn.maNCC
    JOIN ChiTietPhieuNhap ct ON ct.maPN = pn.maPN
    WHERE pn.ngayNhap >= @p_tuNgay
      AND pn.ngayNhap < @p_denNgay
    GROUP BY ncc.maNCC, ncc.tenNCC
    ORDER BY tongChiPhi DESC;
END;
GO

IF OBJECT_ID(N'dbo.sp_BaoCaoChiPhiNhapTheoThang', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE [dbo].[sp_BaoCaoChiPhiNhapTheoThang] AS BEGIN SET NOCOUNT ON; END');
GO
CREATE OR ALTER PROCEDURE sp_BaoCaoChiPhiNhapTheoThang
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        YEAR(pn.ngayNhap) AS nam,
        MONTH(pn.ngayNhap) AS thang,
        SUM(ct.soLuong * ct.donGiaNhap) AS tongChiPhi
    FROM PhieuNhap pn
    JOIN ChiTietPhieuNhap ct ON ct.maPN = pn.maPN
    GROUP BY YEAR(pn.ngayNhap), MONTH(pn.ngayNhap)
    ORDER BY nam, thang;
END;
GO

-- =========================================================
-- 20. MÃ GIẢM GIÁ MẪU (thêm nếu chưa có, chạy lại không lỗi)
-- =========================================================

INSERT INTO MaGiamGia (
    maCode, moTa, loai, giaTri, giamToiDa,
    donToiThieu, soLuong, moiKhachToiDa, batDau, ketThuc
)
SELECT v.maCode, v.moTa, v.loai, v.giaTri, v.giamToiDa,
       v.donToiThieu, v.soLuong, v.moiKhachToiDa, v.batDau, v.ketThuc
FROM (VALUES
    ('WELCOME5', N'Giảm 5% cho đơn từ 1 triệu',         'PHAN_TRAM', 5,      500000, 1000000, NULL, 1, GETDATE(), DATEADD(MONTH, 3, GETDATE())),
    ('GAME200K', N'Giảm 200.000₫ cho đơn từ 3 triệu',    'SO_TIEN',   200000, NULL,   3000000, 100,  1, GETDATE(), DATEADD(MONTH, 3, GETDATE())),
    ('FREESHIP', N'Miễn phí vận chuyển cho đơn từ 500k', 'FREESHIP',  0,      NULL,   500000,  NULL, 3, GETDATE(), DATEADD(MONTH, 3, GETDATE()))
) AS v(maCode, moTa, loai, giaTri, giamToiDa, donToiThieu, soLuong, moiKhachToiDa, batDau, ketThuc)
WHERE NOT EXISTS (SELECT 1 FROM MaGiamGia m WHERE m.maCode = v.maCode);
GO

-- =========================================================
-- 21. DỮ LIỆU MẪU (chỉ chạy khi DB còn trống hoàn toàn)
-- =========================================================

IF NOT EXISTS (
    SELECT 1 FROM LoaiSanPham
    UNION ALL SELECT 1 FROM NenTang
    UNION ALL SELECT 1 FROM Kho
    UNION ALL SELECT 1 FROM NhanVien
    UNION ALL SELECT 1 FROM KhachHang
    UNION ALL SELECT 1 FROM TaiKhoan
    UNION ALL SELECT 1 FROM NhaCungCap
    UNION ALL SELECT 1 FROM SanPham
    UNION ALL SELECT 1 FROM SerialMay
    UNION ALL SELECT 1 FROM TonKho
    UNION ALL SELECT 1 FROM HoaDon
    UNION ALL SELECT 1 FROM ChiTietHoaDon
    UNION ALL SELECT 1 FROM ThanhToan
    UNION ALL SELECT 1 FROM PhieuNhap
    UNION ALL SELECT 1 FROM ChiTietPhieuNhap
    UNION ALL SELECT 1 FROM ChiTietPhieuNhapSerial
    UNION ALL SELECT 1 FROM PhieuThuCu
    UNION ALL SELECT 1 FROM ChiTietPhieuThuCu
    UNION ALL SELECT 1 FROM PhieuBaoHanh
    UNION ALL SELECT 1 FROM BienDongKho
    UNION ALL SELECT 1 FROM LichSuTachMaSanPham
)
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;

        -- Danh mục
        SET IDENTITY_INSERT LoaiSanPham ON;
        INSERT INTO LoaiSanPham(maLoai, tenLoai)
        VALUES
            (1, N'Máy console'),
            (2, N'Đĩa game'),
            (3, N'Tay cầm'),
            (4, N'Phụ kiện'),
            (5, N'Thẻ nhớ');
        SET IDENTITY_INSERT LoaiSanPham OFF;

        SET IDENTITY_INSERT NenTang ON;
        INSERT INTO NenTang(maNenTang, tenNenTang)
        VALUES
            (1, N'PlayStation'),
            (2, N'Xbox'),
            (3, N'Nintendo Switch'),
            (4, N'PC / Steam');
        SET IDENTITY_INSERT NenTang OFF;

        SET IDENTITY_INSERT Kho ON;
        INSERT INTO Kho(maKho, tenKho, diaChi)
        VALUES
            (1, N'Kho chính Quận 7', N'12 Nguyễn Thị Thập, Quận 7, TP.HCM'),
            (2, N'Kho phụ kiện Quận 1', N'45 Lê Lợi, Quận 1, TP.HCM'),
            (3, N'Kho hàng cũ Thủ Đức', N'88 Võ Văn Ngân, TP. Thủ Đức, TP.HCM');
        SET IDENTITY_INSERT Kho OFF;

        -- Nhân viên
        SET IDENTITY_INSERT NhanVien ON;
        INSERT INTO NhanVien (maNV, hoTen, soDienThoai, email, diaChi, chucVu)
        VALUES
            (1, N'Nguyễn Thanh Ngân', N'0901000001', N'ngan.nt@store.vn', N'Quận 7, TP.HCM', N'Quản lý'),
            (2, N'Trần Hữu Nhật', N'0901000002', N'nhat.th@store.vn', N'Quận 1, TP.HCM', N'Thu ngân'),
            (3, N'Lê Phượng Vĩ', N'0901000003', N'vi.lp@store.vn', N'Quận 3, TP.HCM', N'Bán hàng'),
            (4, N'Phạm Thành Long', N'0901000004', N'long.pt@store.vn', N'Quận 5, TP.HCM', N'Bán hàng'),
            (5, N'Võ Quốc Huy', N'0901000005', N'huy.vq@store.vn', N'Quận 7, TP.HCM', N'Thu ngân'),
            (6, N'Đặng Minh Anh', N'0901000006', N'anh.dm@store.vn', N'Quận 10, TP.HCM', N'Bán hàng'),
            (7, N'Bùi Thu Hà', N'0901000007', N'ha.bt@store.vn', N'Bình Thạnh, TP.HCM', N'Kỹ thuật'),
            (8, N'Hoàng Gia Bảo', N'0901000008', N'bao.hg@store.vn', N'Gò Vấp, TP.HCM', N'Kỹ thuật'),
            (9, N'Phan Ngọc Linh', N'0901000009', N'linh.pn@store.vn', N'Tân Bình, TP.HCM', N'Kỹ thuật'),
            (10, N'Vũ Đức Anh', N'0901000010', N'anh.vd@store.vn', N'Quận 4, TP.HCM', N'Kỹ thuật'),
            (11, N'Đỗ Khánh Vy', N'0901000011', N'vy.dk@store.vn', N'Quận 2, TP.HCM', N'Thủ kho'),
            (12, N'Ngô Tuấn Kiệt', N'0901000012', N'kiet.nt@store.vn', N'Thủ Đức, TP.HCM', N'Thủ kho'),
            (13, N'Dương Mỹ Duyên', N'0901000013', N'duyen.dm@store.vn', N'Quận 6, TP.HCM', N'Bán hàng'),
            (14, N'Lý Hoàng Nam', N'0901000014', N'nam.lh@store.vn', N'Quận 8, TP.HCM', N'Bán hàng'),
            (15, N'Trương Bảo Châu', N'0901000015', N'chau.tb@store.vn', N'Phú Nhuận, TP.HCM', N'Thu ngân'),
            (16, N'Huỳnh Anh Tuấn', N'0901000016', N'tuan.ha@store.vn', N'Quận 12, TP.HCM', N'Giao hàng'),
            (17, N'Mai Thanh Tùng', N'0901000017', N'tung.mt@store.vn', N'Hóc Môn, TP.HCM', N'Giao hàng'),
            (18, N'Đinh Thị Hoa', N'0901000018', N'hoa.dt@store.vn', N'Bình Tân, TP.HCM', N'Kế toán'),
            (19, N'Lương Minh Khoa', N'0901000019', N'khoa.lm@store.vn', N'Quận 11, TP.HCM', N'Bán hàng'),
            (20, N'Tạ Quỳnh Như', N'0901000020', N'nhu.tq@store.vn', N'Nhà Bè, TP.HCM', N'Bán hàng');
        SET IDENTITY_INSERT NhanVien OFF;

        -- Khách hàng
        SET IDENTITY_INSERT KhachHang ON;
        INSERT INTO KhachHang (maKH, hoTen, soDienThoai, email, diaChi)
        VALUES
            (1, N'Nguyễn Văn An', N'0911000001', N'an.nv@mail.com', N'Quận 7, TP.HCM'),
            (2, N'Trần Thị Bình', N'0911000002', N'binh.tt@mail.com', N'Quận 1, TP.HCM'),
            (3, N'Lê Hoàng Cường', N'0911000003', N'cuong.lh@mail.com', N'Quận 3, TP.HCM'),
            (4, N'Phạm Thị Dung', N'0911000004', N'dung.pt@mail.com', N'Quận 5, TP.HCM'),
            (5, N'Võ Minh Đức', N'0911000005', N'duc.vm@mail.com', N'Quận 10, TP.HCM'),
            (6, N'Đặng Thu Giang', N'0911000006', N'giang.dt@mail.com', N'Bình Thạnh, TP.HCM'),
            (7, N'Bùi Quang Hải', N'0911000007', N'hai.bq@mail.com', N'Gò Vấp, TP.HCM'),
            (8, N'Hoàng Thị Hạnh', N'0911000008', N'hanh.ht@mail.com', N'Tân Bình, TP.HCM'),
            (9, N'Phan Văn Khải', N'0911000009', N'khai.pv@mail.com', N'Quận 4, TP.HCM'),
            (10, N'Vũ Thị Lan', N'0911000010', N'lan.vt@mail.com', N'Quận 2, TP.HCM'),
            (11, N'Đỗ Minh Long', N'0911000011', N'long.dm@mail.com', N'Thủ Đức, TP.HCM'),
            (12, N'Ngô Thị Mai', N'0911000012', N'mai.nt@mail.com', N'Quận 6, TP.HCM'),
            (13, N'Dương Văn Nam', N'0911000013', N'nam.dv@mail.com', N'Quận 8, TP.HCM'),
            (14, N'Lý Thị Oanh', N'0911000014', N'oanh.lt@mail.com', N'Phú Nhuận, TP.HCM'),
            (15, N'Trương Quốc Phong', N'0911000015', N'phong.tq@mail.com', N'Quận 12, TP.HCM'),
            (16, N'Huỳnh Thị Quyên', N'0911000016', N'quyen.ht@mail.com', N'Hóc Môn, TP.HCM'),
            (17, N'Mai Văn Sơn', N'0911000017', N'son.mv@mail.com', N'Bình Tân, TP.HCM'),
            (18, N'Đinh Thị Thảo', N'0911000018', N'thao.dt@mail.com', N'Quận 11, TP.HCM'),
            (19, N'Lương Văn Tín', N'0911000019', N'tin.lv@mail.com', N'Nhà Bè, TP.HCM'),
            (20, N'Tạ Thị Uyên', N'0911000020', N'uyen.tt@mail.com', N'Cần Giờ, TP.HCM');
        SET IDENTITY_INSERT KhachHang OFF;

        -- Tài khoản mẫu: mật khẩu 123456, lưu BCrypt
        DECLARE @PasswordHash NVARCHAR(255) =
            N'$2a$12$kPX7nJ3TS//82nKTOWPe8.S4.AxbgM4AN2XUMMEE2wzVMVbqKagEW';

        SET IDENTITY_INSERT TaiKhoan ON;
        INSERT INTO TaiKhoan (maTK, tenDangNhap, matKhau, vaiTro, maNV, maKH)
        VALUES
            (1, N'admin', @PasswordHash, N'ADMIN', 1, NULL),
            (2, N'nv02', @PasswordHash, N'NHANVIEN', 2, NULL),
            (3, N'nv03', @PasswordHash, N'NHANVIEN', 3, NULL),
            (4, N'nv04', @PasswordHash, N'NHANVIEN', 4, NULL),
            (5, N'nv05', @PasswordHash, N'NHANVIEN', 5, NULL),
            (6, N'nv06', @PasswordHash, N'NHANVIEN', 6, NULL),
            (7, N'nv07', @PasswordHash, N'NHANVIEN', 7, NULL),
            (8, N'nv08', @PasswordHash, N'NHANVIEN', 8, NULL),
            (9, N'kh01', @PasswordHash, N'KHACHHANG', NULL, 1),
            (10, N'kh02', @PasswordHash, N'KHACHHANG', NULL, 2),
            (11, N'kh03', @PasswordHash, N'KHACHHANG', NULL, 3),
            (12, N'kh04', @PasswordHash, N'KHACHHANG', NULL, 4),
            (13, N'kh05', @PasswordHash, N'KHACHHANG', NULL, 5),
            (14, N'kh06', @PasswordHash, N'KHACHHANG', NULL, 6),
            (15, N'kh07', @PasswordHash, N'KHACHHANG', NULL, 7),
            (16, N'kh08', @PasswordHash, N'KHACHHANG', NULL, 8),
            (17, N'kh09', @PasswordHash, N'KHACHHANG', NULL, 9),
            (18, N'kh10', @PasswordHash, N'KHACHHANG', NULL, 10),
            (19, N'kh11', @PasswordHash, N'KHACHHANG', NULL, 11),
            (20, N'kh12', @PasswordHash, N'KHACHHANG', NULL, 12);
        SET IDENTITY_INSERT TaiKhoan OFF;

        -- Nhà cung cấp
        SET IDENTITY_INSERT NhaCungCap ON;
        INSERT INTO NhaCungCap (maNCC, tenNCC, soDienThoai, email, diaChi)
        VALUES
            (1, N'Sony Interactive Việt Nam', N'0281000001', N'sales@sony-vn.com', N'Quận 1, TP.HCM'),
            (2, N'Microsoft Việt Nam', N'0281000002', N'sales@microsoft-vn.com', N'Quận 1, TP.HCM'),
            (3, N'Nintendo Asia Distribution', N'0281000003', N'sales@nintendo-asia.com', N'Quận 3, TP.HCM'),
            (4, N'Digiworld', N'0281000004', N'sales@digiworld.vn', N'Quận 7, TP.HCM'),
            (5, N'FPT Synnex', N'0281000005', N'sales@fptsynnex.vn', N'Quận 7, TP.HCM'),
            (6, N'Thế Giới Game', N'0281000006', N'sales@tggame.vn', N'Quận 10, TP.HCM'),
            (7, N'Khang Gaming', N'0281000007', N'sales@khanggaming.vn', N'Quận 5, TP.HCM'),
            (8, N'Hoàng Hà Distribution', N'0281000008', N'sales@hoangha.vn', N'Quận 3, TP.HCM'),
            (9, N'An Phát Gaming', N'0281000009', N'sales@anphat.vn', N'Tân Bình, TP.HCM'),
            (10, N'Phong Vũ Trading', N'0281000010', N'sales@phongvu.vn', N'Quận 1, TP.HCM'),
            (11, N'GearVN Wholesale', N'0281000011', N'sales@gearvn.vn', N'Quận 10, TP.HCM'),
            (12, N'Tân Long Group', N'0281000012', N'sales@tanlong.vn', N'Quận 5, TP.HCM'),
            (13, N'Bluestar Gaming', N'0281000013', N'sales@bluestar.vn', N'Gò Vấp, TP.HCM'),
            (14, N'Mai Nguyên Trading', N'0281000014', N'sales@mainguyen.vn', N'Quận 6, TP.HCM'),
            (15, N'Game Zone Supply', N'0281000015', N'sales@gamezone.vn', N'Bình Thạnh, TP.HCM'),
            (16, N'Saigon Gamer', N'0281000016', N'sales@saigongamer.vn', N'Quận 4, TP.HCM'),
            (17, N'Hà Nội Console', N'0241000017', N'sales@hnconsole.vn', N'Hà Nội'),
            (18, N'Đại Dương Gaming', N'0236000018', N'sales@daiduong.vn', N'Đà Nẵng'),
            (19, N'Pixel Trade', N'0281000019', N'sales@pixeltrade.vn', N'Phú Nhuận, TP.HCM'),
            (20, N'Nova Distribution', N'0281000020', N'sales@nova.vn', N'Quận 2, TP.HCM');
        SET IDENTITY_INSERT NhaCungCap OFF;

        -- Sản phẩm: mã hàng mới trước, mã hàng cũ (có maSPGoc) sau
        SET IDENTITY_INSERT SanPham ON;

        INSERT INTO SanPham (
            maSP, tenSP, moTa, giaBan, maLoai,
            maNenTang, hinhAnh, tinhTrang, maSPGoc
        )
        VALUES
            (1, N'PlayStation 5 Standard', N'PS5 bản đĩa 825GB', 13990000, 1, 1, N'SP1.jpg', N'MOI', NULL),
            (2, N'PlayStation 5 Digital', N'PS5 bản không ổ đĩa', 11990000, 1, 1, N'SP2.jpg', N'MOI', NULL),
            (3, N'PlayStation 5 Pro', N'PS5 Pro 2TB', 18990000, 1, 1, N'SP3.jpg', N'MOI', NULL),
            (4, N'Xbox Series X', N'Xbox Series X 1TB', 12990000, 1, 2, N'SP4.jpg', N'MOI', NULL),
            (5, N'Xbox Series S', N'Xbox Series S 512GB', 7490000, 1, 2, N'SP5.jpg', N'MOI', NULL),
            (6, N'Nintendo Switch OLED', N'Switch màn hình OLED 7 inch', 8490000, 1, 3, N'SP6.jpg', N'MOI', NULL),
            (7, N'Nintendo Switch Lite', N'Switch Lite cầm tay', 5290000, 1, 3, N'SP7.jpg', N'MOI', NULL),
            (8, N'Steam Deck OLED', N'Steam Deck OLED 512GB', 17990000, 1, 4, N'SP8.jpg', N'MOI', NULL),
            (9, N'PlayStation 4 Slim', N'PS4 Slim 1TB đã qua sử dụng', 4500000, 1, 1, N'SP9.jpg', N'CU', NULL),
            (10, N'Nintendo Switch 2', N'Switch thế hệ 2', 12990000, 1, 3, N'SP10.jpg', N'MOI', NULL),
            (11, N'God of War Ragnarök (PS5)', N'Đĩa game PS5 mới', 1250000, 2, 1, N'SP11.jpg', N'MOI', NULL),
            (12, N'Marvel''s Spider-Man 2 (PS5)', N'Đĩa game PS5 mới', 1390000, 2, 1, N'SP12.jpg', N'MOI', NULL),
            (13, N'Zelda: Tears of the Kingdom', N'Game Nintendo Switch mới', 1290000, 2, 3, N'SP13.jpg', N'MOI', NULL),
            (14, N'Mario Kart 8 Deluxe', N'Game Nintendo Switch mới', 1190000, 2, 3, N'SP14.jpg', N'MOI', NULL),
            (15, N'Forza Horizon 5 (Xbox)', N'Đĩa game Xbox mới', 1090000, 2, 2, N'SP15.jpg', N'MOI', NULL),
            (16, N'Halo Infinite (Xbox)', N'Đĩa game Xbox mới', 790000, 2, 2, N'SP16.jpg', N'MOI', NULL),
            (17, N'EA Sports FC 25 (PS5)', N'Đĩa game PS5 mới', 1190000, 2, 1, N'SP17.jpg', N'MOI', NULL),
            (18, N'Elden Ring (PS5)', N'Đĩa game PS5 mới', 990000, 2, 1, N'SP18.jpg', N'MOI', NULL),
            (19, N'Tay cầm DualSense', N'Tay cầm PS5 chính hãng mới', 1890000, 3, 1, N'SP19.jpg', N'MOI', NULL),
            (20, N'Nintendo Switch Pro Controller', N'Tay cầm Switch chính hãng mới', 1590000, 3, 3, N'SP20.jpg', N'MOI', NULL);

        INSERT INTO SanPham (
            maSP, tenSP, moTa, giaBan, maLoai,
            maNenTang, hinhAnh, tinhTrang, maSPGoc
        )
        VALUES
            (21, N'Nintendo Switch OLED', N'Switch OLED đã qua sử dụng, đã kiểm tra hoạt động', 6490000, 1, 3, N'SP6.jpg', N'CU', 6),
            (22, N'Nintendo Switch Lite', N'Switch Lite đã qua sử dụng, đã kiểm tra hoạt động', 3790000, 1, 3, N'SP7.jpg', N'CU', 7),
            (23, N'Tay cầm DualSense', N'DualSense đã qua sử dụng, đã kiểm tra nút và analog', 1190000, 3, 1, N'SP19.jpg', N'CU', 19),
            (24, N'Nintendo Switch Pro Controller', N'Pro Controller đã qua sử dụng, đã kiểm tra hoạt động', 990000, 3, 3, N'SP20.jpg', N'CU', 20);

        SET IDENTITY_INSERT SanPham OFF;

        -- Serial máy nhập hàng
        SET IDENTITY_INSERT SerialMay ON;

        INSERT INTO SerialMay (maSerial, maSP, soSerial, tinhTrang)
        VALUES
            (1, 1, N'SN-PS5STD-0001', N'MOI'),
            (2, 1, N'SN-PS5STD-0002', N'MOI'),
            (3, 2, N'SN-PS5DIG-0001', N'MOI'),
            (4, 2, N'SN-PS5DIG-0002', N'MOI'),
            (5, 3, N'SN-PS5PRO-0001', N'MOI'),
            (6, 3, N'SN-PS5PRO-0002', N'MOI'),
            (7, 4, N'SN-XSX-0001', N'MOI'),
            (8, 4, N'SN-XSX-0002', N'MOI'),
            (9, 5, N'SN-XSS-0001', N'MOI'),
            (10, 5, N'SN-XSS-0002', N'MOI'),
            (11, 6, N'SN-NSWOLED-0001', N'MOI'),
            (12, 6, N'SN-NSWOLED-0002', N'MOI'),
            (13, 7, N'SN-NSWLITE-0001', N'MOI'),
            (14, 7, N'SN-NSWLITE-0002', N'MOI'),
            (15, 8, N'SN-STEAMDECK-0001', N'MOI'),
            (16, 8, N'SN-STEAMDECK-0002', N'MOI'),
            (17, 9, N'SN-PS4SLIM-0001', N'CU'),
            (18, 9, N'SN-PS4SLIM-0002', N'CU'),
            (19, 10, N'SN-NSW2-0001', N'MOI'),
            (20, 10, N'SN-NSW2-0002', N'MOI');

        INSERT INTO SerialMay (maSerial, maSP, soSerial, tinhTrang)
        SELECT
            25 + ROW_NUMBER() OVER (ORDER BY sp.maSP, n.soThuTu),
            sp.maSP,
            CONCAT(
                N'SN-TEST-', sp.maSP, N'-',
                RIGHT(N'0000' + CONVERT(NVARCHAR(4), n.soThuTu), 4)
            ),
            sp.tinhTrang
        FROM SanPham sp
        CROSS JOIN (
            VALUES (3), (4), (5), (6), (7), (8), (9), (10), (11),
                   (12), (13), (14), (15), (16), (17), (18), (19), (20)
        ) n(soThuTu)
        WHERE sp.maSP IN (2, 4, 5, 6, 7, 8, 9, 10);

        SET IDENTITY_INSERT SerialMay OFF;

        -- Phiếu nhập
        SET IDENTITY_INSERT PhieuNhap ON;
        INSERT INTO PhieuNhap (maPN, maNCC, maNV, maKho, ghiChu)
        VALUES
            (1, 1, 3, 1, N'Nhập PS5 Standard'),
            (2, 2, 4, 1, N'Nhập PS5 Digital'),
            (3, 3, 5, 1, N'Nhập PS5 Pro'),
            (4, 4, 6, 1, N'Nhập Xbox Series X'),
            (5, 5, 2, 1, N'Nhập Xbox Series S'),
            (6, 6, 3, 1, N'Nhập Switch OLED'),
            (7, 7, 4, 1, N'Nhập Switch Lite'),
            (8, 8, 5, 1, N'Nhập Steam Deck'),
            (9, 9, 6, 1, N'Nhập PS4 Slim cũ đã kiểm tra'),
            (10, 10, 2, 1, N'Nhập Switch 2'),
            (11, 11, 3, 1, N'Nhập God of War'),
            (12, 12, 4, 1, N'Nhập Spider-Man 2'),
            (13, 13, 5, 1, N'Nhập Zelda TotK'),
            (14, 14, 6, 1, N'Nhập Mario Kart 8'),
            (15, 15, 2, 1, N'Nhập Forza Horizon 5'),
            (16, 16, 3, 1, N'Nhập Halo Infinite'),
            (17, 17, 4, 1, N'Nhập EA FC 25'),
            (18, 18, 5, 1, N'Nhập Elden Ring'),
            (19, 19, 6, 2, N'Nhập DualSense'),
            (20, 20, 2, 2, N'Nhập Pro Controller');
        SET IDENTITY_INSERT PhieuNhap OFF;

        -- Trigger tự tạo tồn kho và tổng tiền
        SET IDENTITY_INSERT ChiTietPhieuNhap ON;
        INSERT INTO ChiTietPhieuNhap (maCTPN, maPN, maSP, soLuong, donGiaNhap)
        VALUES
            (1, 1, 1, 2, 11500000),
            (2, 2, 2, 20, 9900000),
            (3, 3, 3, 2, 16000000),
            (4, 4, 4, 20, 10800000),
            (5, 5, 5, 20, 6100000),
            (6, 6, 6, 20, 7000000),
            (7, 7, 7, 20, 4300000),
            (8, 8, 8, 20, 15200000),
            (9, 9, 9, 20, 3300000),
            (10, 10, 10, 20, 10900000),
            (11, 11, 11, 30, 1000000),
            (12, 12, 12, 30, 1100000),
            (13, 13, 13, 30, 1050000),
            (14, 14, 14, 30, 950000),
            (15, 15, 15, 30, 870000),
            (16, 16, 16, 30, 620000),
            (17, 17, 17, 30, 950000),
            (18, 18, 18, 30, 790000),
            (19, 19, 19, 20, 1500000),
            (20, 20, 20, 20, 1250000);
        SET IDENTITY_INSERT ChiTietPhieuNhap OFF;

        INSERT INTO ChiTietPhieuNhapSerial (maCTPN, maSerial)
        SELECT ct.maCTPN, s.maSerial
        FROM ChiTietPhieuNhap ct
        JOIN SerialMay s ON s.maSP = ct.maSP
        WHERE ct.maCTPN BETWEEN 1 AND 10
          AND (s.maSerial BETWEEN 1 AND 20 OR s.soSerial LIKE N'SN-TEST-%');

        -- Hóa đơn mẫu
        DECLARE @HoaDonMau TABLE (
            maHD BIGINT PRIMARY KEY,
            maKH BIGINT NOT NULL,
            maNV BIGINT NULL,
            maKho BIGINT NOT NULL,
            kenhBan NVARCHAR(20) NOT NULL,
            trangThaiDich NVARCHAR(20) NOT NULL,
            ghiChu NVARCHAR(255) NULL
        );

        INSERT INTO @HoaDonMau (maHD, maKH, maNV, maKho, kenhBan, trangThaiDich, ghiChu)
        VALUES
            (1, 1, 3, 1, N'QUAY', N'HOAN_TAT', N'Mua tại quầy'),
            (2, 2, NULL, 1, N'ONLINE', N'HOAN_TAT', N'Giao hàng nhanh'),
            (3, 3, 4, 1, N'QUAY', N'HOAN_TAT', NULL),
            (4, 4, NULL, 1, N'ONLINE', N'HOAN_TAT', NULL),
            (5, 5, 5, 1, N'QUAY', N'HOAN_TAT', NULL),
            (6, 6, NULL, 1, N'ONLINE', N'HOAN_TAT', NULL),
            (7, 7, 6, 1, N'QUAY', N'HOAN_TAT', NULL),
            (8, 8, NULL, 1, N'ONLINE', N'HOAN_TAT', NULL),
            (9, 9, 2, 1, N'QUAY', N'HOAN_TAT', NULL),
            (10, 10, NULL, 1, N'ONLINE', N'HOAN_TAT', NULL),
            (11, 11, 3, 1, N'QUAY', N'HOAN_TAT', NULL),
            (12, 12, 4, 1, N'QUAY', N'HOAN_TAT', NULL),
            (13, 13, NULL, 1, N'ONLINE', N'HOAN_TAT', NULL),
            (14, 14, 5, 1, N'QUAY', N'HOAN_TAT', NULL),
            (15, 15, NULL, 1, N'ONLINE', N'DANG_GIAO', N'Đang giao'),
            (16, 16, NULL, 1, N'ONLINE', N'CHO_XAC_NHAN', N'Chờ thanh toán'),
            (17, 17, 6, 1, N'QUAY', N'HOAN_TAT', NULL),
            (18, 18, NULL, 1, N'ONLINE', N'DA_XAC_NHAN', N'Đã xác nhận'),
            (19, 19, 2, 2, N'QUAY', N'HOAN_TAT', NULL),
            (20, 20, 3, 2, N'QUAY', N'HUY', N'Khách hủy trước khi thanh toán');

        SET IDENTITY_INSERT HoaDon ON;
        INSERT INTO HoaDon (
            maHD, maKH, maNV, maKho, kenhBan, trangThai,
            tenNguoiNhan, soDienThoaiNhan, diaChi, ghiChu, maYeuCau
        )
        SELECT
            m.maHD,
            m.maKH,
            m.maNV,
            CASE WHEN m.kenhBan = N'QUAY' THEN m.maKho ELSE NULL END,
            m.kenhBan,
            N'CHO_XAC_NHAN',
            k.hoTen,
            k.soDienThoai,
            CASE WHEN m.kenhBan = N'ONLINE' THEN k.diaChi ELSE NULL END,
            m.ghiChu,
            CONVERT(VARCHAR(36), NEWID())
        FROM @HoaDonMau m
        JOIN KhachHang k ON k.maKH = m.maKH;
        SET IDENTITY_INSERT HoaDon OFF;

        SET IDENTITY_INSERT ChiTietHoaDon ON;
        INSERT INTO ChiTietHoaDon (maCTHD, maHD, maSP, maKho, maSerial, soLuong, donGia)
        SELECT d.maCTHD, d.maHD, d.maSP, m.maKho, d.maSerial, d.soLuong, d.donGia
        FROM (
            VALUES
                (1, 1, 1, 1, 1, 13990000),
                (2, 2, 2, 3, 1, 11990000),
                (3, 3, 3, 5, 1, 18990000),
                (4, 4, 4, 7, 1, 12990000),
                (5, 5, 5, 9, 1, 7490000),
                (6, 6, 6, 11, 1, 8490000),
                (7, 7, 7, 13, 1, 5290000),
                (8, 8, 8, 15, 1, 17990000),
                (9, 9, 9, 17, 1, 4500000),
                (10, 10, 10, 19, 1, 12990000),
                (11, 11, 11, NULL, 2, 1250000),
                (12, 12, 12, NULL, 1, 1390000),
                (13, 13, 13, NULL, 3, 1290000),
                (14, 14, 14, NULL, 1, 1190000),
                (15, 15, 15, NULL, 2, 1090000),
                (16, 16, 16, NULL, 1, 790000),
                (17, 17, 17, NULL, 1, 1190000),
                (18, 18, 18, NULL, 2, 990000),
                (19, 19, 19, NULL, 1, 1890000),
                (20, 20, 20, NULL, 1, 1590000)
        ) d(maCTHD, maHD, maSP, maSerial, soLuong, donGia)
        JOIN @HoaDonMau m ON m.maHD = d.maHD;
        SET IDENTITY_INSERT ChiTietHoaDon OFF;

        INSERT INTO ThanhToan (maHD, phuongThuc, soTien, trangThai, ngayThanhToan)
        SELECT
            h.maHD,
            CASE h.maHD % 3
                WHEN 0 THEN N'CHUYEN_KHOAN'
                WHEN 1 THEN N'MOMO'
                ELSE N'ZALOPAY'
            END,
            h.tongTien,
            CASE
                WHEN m.trangThaiDich IN (N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT')
                THEN N'DA_THANH_TOAN'
                ELSE N'CHUA_THANH_TOAN'
            END,
            CASE
                WHEN m.trangThaiDich IN (N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT')
                THEN h.ngayLap
                ELSE NULL
            END
        FROM HoaDon h
        JOIN @HoaDonMau m ON m.maHD = h.maHD;

        UPDATE h
        SET h.trangThai = N'DA_XAC_NHAN'
        FROM HoaDon h
        JOIN @HoaDonMau m ON m.maHD = h.maHD
        WHERE m.trangThaiDich IN (N'DA_XAC_NHAN', N'DANG_GIAO', N'HOAN_TAT');

        UPDATE h
        SET h.trangThai = N'DANG_GIAO'
        FROM HoaDon h
        JOIN @HoaDonMau m ON m.maHD = h.maHD
        WHERE m.trangThaiDich = N'DANG_GIAO';

        UPDATE h
        SET h.trangThai = N'HOAN_TAT'
        FROM HoaDon h
        JOIN @HoaDonMau m ON m.maHD = h.maHD
        WHERE m.trangThaiDich = N'HOAN_TAT';

        UPDATE h
        SET h.trangThai = N'HUY'
        FROM HoaDon h
        JOIN @HoaDonMau m ON m.maHD = h.maHD
        WHERE m.trangThaiDich = N'HUY';

        -- Phiếu thu cũ nhập về kho 3
        SET IDENTITY_INSERT PhieuThuCu ON;
        INSERT INTO PhieuThuCu (maPTC, maKH, maNV, maHD, maKho, ghiChu)
        VALUES
            (1, 1, 3, 1, 3, N'Thu PS4 cũ khi mua PS5'),
            (2, 2, 4, 2, 3, N'Thu Switch Lite cũ'),
            (3, 3, 5, 3, 3, N'Thu Switch OLED cũ'),
            (4, 4, 6, 4, 3, N'Thu tay cầm cũ'),
            (5, 5, 2, 5, 3, N'Thu tay cầm cũ'),
            (6, 6, 3, 6, 3, N'Thu tay cầm cũ'),
            (7, 7, 4, 7, 3, N'Thu tay cầm cũ'),
            (8, 8, 5, 8, 3, N'Thu tay cầm cũ'),
            (9, 9, 6, 9, 3, N'Thu tay cầm cũ'),
            (10, 10, 2, 10, 3, N'Thu tay cầm cũ'),
            (11, 11, 3, NULL, 3, N'Thu tay cầm cũ'),
            (12, 12, 4, NULL, 3, N'Thu tay cầm cũ'),
            (13, 13, 5, NULL, 3, N'Thu tay cầm cũ'),
            (14, 14, 6, NULL, 3, N'Thu PS4 cũ'),
            (15, 15, 2, NULL, 3, N'Thu Switch Lite cũ'),
            (16, 16, 3, NULL, 3, N'Thu tay cầm cũ'),
            (17, 17, 4, NULL, 3, N'Thu tay cầm cũ'),
            (18, 18, 5, NULL, 3, N'Thu tay cầm cũ'),
            (19, 19, 6, NULL, 3, N'Thu tay cầm cũ'),
            (20, 20, 2, NULL, 3, N'Thu tay cầm cũ');
        SET IDENTITY_INSERT PhieuThuCu OFF;

        SET IDENTITY_INSERT ChiTietPhieuThuCu ON;
        INSERT INTO ChiTietPhieuThuCu (
            maCTTC, maPTC, maSP, soLuong, donGiaThu, tinhTrang, ghiChu
        )
        VALUES
            (1, 1, 9, 1, 2000000, N'CU', N'Máy chạy tốt, trầy nhẹ'),
            (2, 2, 22, 1, 2500000, N'CU', N'Còn bảo hành 3 tháng'),
            (3, 3, 21, 1, 4500000, N'CU', N'Máy đẹp 95%'),
            (4, 4, 23, 2, 600000, N'CU', NULL),
            (5, 5, 24, 1, 700000, N'CU', NULL),
            (6, 6, 23, 2, 650000, N'CU', NULL),
            (7, 7, 24, 1, 600000, N'CU', NULL),
            (8, 8, 23, 2, 450000, N'CU', NULL),
            (9, 9, 24, 1, 300000, N'CU', NULL),
            (10, 10, 23, 3, 400000, N'CU', NULL),
            (11, 11, 24, 1, 500000, N'CU', NULL),
            (12, 12, 23, 2, 800000, N'CU', N'Đã sửa analog và kiểm tra lại'),
            (13, 13, 24, 1, 700000, N'CU', NULL),
            (14, 14, 9, 1, 1800000, N'CU', N'Có vết xước'),
            (15, 15, 22, 1, 2300000, N'CU', NULL),
            (16, 16, 23, 1, 550000, N'CU', NULL),
            (17, 17, 24, 1, 600000, N'CU', NULL),
            (18, 18, 23, 2, 380000, N'CU', NULL),
            (19, 19, 23, 1, 750000, N'CU', NULL),
            (20, 20, 23, 2, 650000, N'CU', NULL);
        SET IDENTITY_INSERT ChiTietPhieuThuCu OFF;

        -- Serial tương ứng với các máy thu cũ
        SET IDENTITY_INSERT SerialMay ON;
        INSERT INTO SerialMay (maSerial, maSP, maKho, soSerial, tinhTrang, trangThai)
        VALUES
            (21, 9, 3, N'SN-PTC01-PS4-0001', N'CU', N'TRONG_KHO'),
            (22, 9, 3, N'SN-PTC14-PS4-0001', N'CU', N'TRONG_KHO'),
            (23, 21, 3, N'SN-PTC03-OLED-0001', N'CU', N'TRONG_KHO'),
            (24, 22, 3, N'SN-PTC02-LITE-0001', N'CU', N'TRONG_KHO'),
            (25, 22, 3, N'SN-PTC15-LITE-0001', N'CU', N'TRONG_KHO');
        SET IDENTITY_INSERT SerialMay OFF;

        -- Bảo hành mẫu
        SET IDENTITY_INSERT PhieuBaoHanh ON;
        INSERT INTO PhieuBaoHanh (
            maBH, maSerial, maNV, ngayHenTra,
            noiDung, moTaLoi, chiPhi, trangThai
        )
        VALUES
            (1, 1, 7, DATEADD(DAY, 7, GETDATE()), N'Vệ sinh quạt tản nhiệt', N'Máy ồn quạt', 0, N'HOAN_TAT'),
            (2, 3, 8, DATEADD(DAY, 7, GETDATE()), N'Thay cáp HDMI', N'Mất tín hiệu hình', 0, N'HOAN_TAT'),
            (3, 5, 9, DATEADD(DAY, 7, GETDATE()), N'Cập nhật firmware', N'Treo logo', 0, N'HOAN_TAT'),
            (4, 7, 10, DATEADD(DAY, 7, GETDATE()), N'Thay nguồn', N'Không lên nguồn', 0, N'HOAN_TAT'),
            (5, 9, 7, DATEADD(DAY, 7, GETDATE()), N'Vệ sinh máy', N'Máy nóng', 0, N'HOAN_TAT'),
            (6, 11, 8, DATEADD(DAY, 7, GETDATE()), N'Thay joycon', N'Drift analog', 0, N'HOAN_TAT'),
            (7, 13, 9, DATEADD(DAY, 7, GETDATE()), N'Thay pin', N'Pin chai nhanh', 0, N'HOAN_TAT'),
            (8, 15, 10, DATEADD(DAY, 7, GETDATE()), N'Thay màn hình', N'Sọc màn hình', 0, N'HOAN_TAT'),
            (9, 17, 7, DATEADD(DAY, 7, GETDATE()), N'Thay ổ cứng', N'Báo lỗi ổ cứng', 350000, N'HOAN_TAT'),
            (10, 19, 8, DATEADD(DAY, 7, GETDATE()), N'Cập nhật hệ thống', N'Lỗi cập nhật', 0, N'HOAN_TAT'),
            (11, 1, 9, DATEADD(DAY, 5, GETDATE()), N'Thay keo tản nhiệt', N'Máy nóng khi chơi game', 0, N'HOAN_TAT'),
            (12, 3, 10, DATEADD(DAY, 5, GETDATE()), N'Thay ổ đĩa', N'Không đọc đĩa', 0, N'HOAN_TAT'),
            (13, 5, 7, DATEADD(DAY, 5, GETDATE()), N'Kiểm tra cổng USB', N'USB không nhận', 0, N'HOAN_TAT'),
            (14, 7, 8, DATEADD(DAY, 5, GETDATE()), N'Thay quạt', N'Quạt kêu to', 0, N'HOAN_TAT'),
            (15, 9, 9, DATEADD(DAY, 5, GETDATE()), N'Đang kiểm tra bo mạch', N'Tự khởi động lại', 0, N'DANG_XU_LY'),
            (16, 11, 10, DATEADD(DAY, 5, GETDATE()), N'Đang chờ linh kiện', N'Hỏng nút bấm', 0, N'DANG_XU_LY'),
            (17, 13, 7, DATEADD(DAY, 5, GETDATE()), N'Đang thay cổng sạc', N'Không sạc được', 0, N'DANG_XU_LY'),
            (18, 15, 8, DATEADD(DAY, 7, GETDATE()), N'Chờ kiểm tra', N'Mất âm thanh', 0, N'TIEP_NHAN'),
            (19, 17, 9, DATEADD(DAY, 7, GETDATE()), N'Chờ kiểm tra', N'Không đọc đĩa', 0, N'TIEP_NHAN'),
            (20, 19, 10, DATEADD(DAY, 7, GETDATE()), N'Chờ kiểm tra', N'Lỗi wifi', 0, N'TIEP_NHAN');
        SET IDENTITY_INSERT PhieuBaoHanh OFF;

        COMMIT TRANSACTION;

        PRINT N'Khởi tạo dữ liệu mẫu thành công.';
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        DECLARE @tableName SYSNAME;
        DECLARE @cleanupSql NVARCHAR(300);

        DECLARE identity_cleanup CURSOR LOCAL FAST_FORWARD FOR
            SELECT t.name
            FROM sys.tables t
            JOIN sys.identity_columns c ON c.object_id = t.object_id
            WHERE t.name IN (
                N'LoaiSanPham', N'NenTang', N'Kho',
                N'NhanVien', N'KhachHang', N'TaiKhoan',
                N'NhaCungCap', N'SanPham', N'SerialMay',
                N'TonKho', N'HoaDon', N'ChiTietHoaDon',
                N'ThanhToan', N'PhieuNhap', N'ChiTietPhieuNhap',
                N'PhieuThuCu', N'ChiTietPhieuThuCu',
                N'PhieuBaoHanh', N'BienDongKho',
                N'LichSuTachMaSanPham'
            );

        OPEN identity_cleanup;
        FETCH NEXT FROM identity_cleanup INTO @tableName;

        WHILE @@FETCH_STATUS = 0
        BEGIN
            SET @cleanupSql =
                N'SET IDENTITY_INSERT ' + QUOTENAME(@tableName) + N' OFF;';

            BEGIN TRY
                EXEC sys.sp_executesql @cleanupSql;
            END TRY
            BEGIN CATCH
                PRINT N'Chưa tắt IDENTITY_INSERT của ' + @tableName
                    + N': ' + ERROR_MESSAGE();
            END CATCH;

            FETCH NEXT FROM identity_cleanup INTO @tableName;
        END;

        CLOSE identity_cleanup;
        DEALLOCATE identity_cleanup;

        ;THROW;
    END CATCH;
END
ELSE
BEGIN
    PRINT N'Database đã có dữ liệu; bỏ qua khởi tạo dữ liệu mẫu.';
END;
GO

-- =========================================================
-- 22. TRIGGER LỊCH SỬ TRẠNG THÁI HÓA ĐƠN
--     (tạo sau dữ liệu mẫu, chỉ ghi lịch sử từ lúc này trở đi)
-- =========================================================

CREATE OR ALTER TRIGGER TRIGGER_HoaDon_LichSuTrangThai
ON HoaDon
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO LichSuTrangThaiHoaDon (maHD, trangThaiCu, trangThaiMoi, maNV)
    SELECT i.maHD,
           d.trangThai,
           i.trangThai,
           CASE WHEN UPDATE(maNV) THEN i.maNV END
    FROM inserted i
    LEFT JOIN deleted d ON d.maHD = i.maHD
    WHERE d.maHD IS NULL
       OR d.trangThai <> i.trangThai;
END;
GO

-- =========================================================
-- 23. KIỂM TRA SAU KHỞI TẠO (chỉ SELECT, không sửa dữ liệu)
-- =========================================================

SELECT N'SanPham' AS tenBang, COUNT_BIG(*) AS soDong FROM SanPham
UNION ALL SELECT N'SerialMay', COUNT_BIG(*) FROM SerialMay
UNION ALL SELECT N'HoaDon', COUNT_BIG(*) FROM HoaDon
UNION ALL SELECT N'ChiTietHoaDon', COUNT_BIG(*) FROM ChiTietHoaDon
UNION ALL SELECT N'ThanhToan', COUNT_BIG(*) FROM ThanhToan
UNION ALL SELECT N'TonKho', COUNT_BIG(*) FROM TonKho
UNION ALL SELECT N'MaGiamGia', COUNT_BIG(*) FROM MaGiamGia;

SELECT
    COUNT_BIG(*) AS tongSoMaSanPham,
    SUM(CASE WHEN tinhTrang = N'MOI' THEN 1 ELSE 0 END) AS soMaHangMoi,
    SUM(CASE WHEN tinhTrang = N'CU' THEN 1 ELSE 0 END) AS soMaHangCu
FROM SanPham;

SELECT
    moi.maSP AS maHangMoi,
    cu.maSP AS maHangCu,
    moi.tenSP,
    moi.giaBan AS giaMoi,
    cu.giaBan AS giaCu,
    moi.giaBan - cu.giaBan AS chenhLech
FROM SanPham cu
JOIN SanPham moi ON moi.maSP = cu.maSPGoc
ORDER BY moi.maSP;

SELECT trangThai, COUNT_BIG(*) AS soHoaDon
FROM HoaDon
GROUP BY trangThai;

SELECT phuongThuc, trangThai, COUNT_BIG(*) AS soThanhToan
FROM ThanhToan
GROUP BY phuongThuc, trangThai
ORDER BY phuongThuc, trangThai;

SELECT SUM(CONVERT(BIGINT, soLuong)) AS tongTonKhaDung
FROM TonKho;

SELECT
    k.tenKho, sp.maSP, sp.tenSP, sp.tinhTrang, tk.soLuong
FROM TonKho tk
JOIN Kho k ON k.maKho = tk.maKho
JOIN SanPham sp ON sp.maSP = tk.maSP
ORDER BY k.maKho, sp.maSP;

-- Chi tiết hóa đơn thiếu kho (kỳ vọng: 0 dòng)
SELECT maCTHD, maHD, maSP
FROM ChiTietHoaDon
WHERE maKho IS NULL;

-- Hóa đơn lệch tổng tiền (kỳ vọng: 0 dòng với dữ liệu mẫu)
SELECT
    h.maHD,
    h.tongTien,
    COALESCE(SUM(c.soLuong * c.donGia), 0) AS tongChiTiet,
    h.phiVanChuyen,
    h.giamGia
FROM HoaDon h
LEFT JOIN ChiTietHoaDon c ON c.maHD = h.maHD
GROUP BY h.maHD, h.tongTien, h.phiVanChuyen, h.giamGia
HAVING h.tongTien <> COALESCE(SUM(c.soLuong * c.donGia), 0) + h.phiVanChuyen - h.giamGia;

-- Thanh toán lệch số tiền (kỳ vọng: 0 dòng)
SELECT t.maTT, t.maHD, t.soTien, h.tongTien
FROM ThanhToan t
JOIN HoaDon h ON h.maHD = t.maHD
WHERE t.soTien <> h.tongTien;

-- Tài khoản vai trò mới
SELECT
    tenDangNhap,
    vaiTro,
    LEN(matKhau) AS DoDaiMatKhau,
    LEFT(matKhau, 4) AS DauMaHoa
FROM dbo.TaiKhoan
WHERE vaiTro IN (N'BANHANG', N'KHO', N'BAOHANH');
GO
USE QLStoreConsole;
GO

SELECT h.maHD, h.kenhBan, h.trangThai, c.maSP, sp.tenSP,
       CASE WHEN h.trangThai = N'HOAN_TAT'
            THEN N'Được đánh giá'
            ELSE N'Chưa được (đơn chưa hoàn tất)' END AS ketQua
FROM TaiKhoan tk
JOIN HoaDon h ON h.maKH = tk.maKH
JOIN ChiTietHoaDon c ON c.maHD = h.maHD
JOIN SanPham sp ON sp.maSP = c.maSP
WHERE tk.tenDangNhap = N'kh01'
ORDER BY h.maHD;
GO

INSERT INTO DanhGia (maSP, maKH, soSao, noiDung)
SELECT v.maSP, v.maKH, v.soSao, v.noiDung
FROM (VALUES
    (2,  2,  5, N'PS5 Digital chạy êm, tải game nhanh, đóng gói cẩn thận.'),
    (3,  3,  5, N'PS5 Pro hình ảnh rất đẹp, đáng tiền cho người chơi lâu năm.'),
    (4,  4,  4, N'Xbox Series X mạnh, hơi nóng khi chơi lâu nhưng vẫn ổn.'),
    (5,  5,  4, N'Series S nhỏ gọn, phù hợp phòng nhỏ, giá hợp lý.'),
    (6,  6,  5, N'Switch OLED màn hình đẹp, cầm lâu không bị mỏi tay.'),
    (7,  7,  4, N'Switch Lite gọn nhẹ, mang đi du lịch rất tiện.'),
    (8,  8,  5, N'Steam Deck chơi được nhiều game PC, pin dùng khá ổn.'),
    (9,  9,  3, N'PS4 Slim cũ chạy ổn, vỏ có vài vết xước nhỏ như mô tả.'),
    (10, 10, 5, N'Switch 2 mượt hơn hẳn đời đầu, giao hàng nhanh.'),
    (11, 11, 5, N'God of War Ragnarök đĩa mới nguyên seal, game quá hay.'),
    (12, 12, 4, N'Spider-Man 2 đồ họa đẹp, cốt truyện cuốn từ đầu.'),
    (13, 13, 5, N'Zelda Tears of the Kingdom đĩa chính hãng, chơi rất cuốn.'),
    (14, 14, 4, N'Mario Kart 8 Deluxe chơi nhóm vui, phù hợp cả gia đình.')
) AS v(maSP, maKH, soSao, noiDung)
WHERE EXISTS (SELECT 1 FROM SanPham s WHERE s.maSP = v.maSP)
  AND EXISTS (SELECT 1 FROM KhachHang k WHERE k.maKH = v.maKH)
  AND NOT EXISTS (
      SELECT 1 FROM DanhGia d
      WHERE d.maSP = v.maSP AND d.maKH = v.maKH
  );
GO

SELECT d.maSP, sp.tenSP, k.hoTen, d.soSao, d.ngayTao
FROM DanhGia d
JOIN SanPham sp ON sp.maSP = d.maSP
JOIN KhachHang k ON k.maKH = d.maKH
ORDER BY d.maSP;
GO