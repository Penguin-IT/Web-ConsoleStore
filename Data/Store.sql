Create database QLStoreConsole;
use QLStoreConsole;
CREATE TABLE Kho (
    maKho BIGINT AUTO_INCREMENT PRIMARY KEY
    
);
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


--==============================Phượng Vĩ( Phiếu Thu cũ, chi tiết phiếu Thu cũ, phiếu Bảo hành)============

--1 Phiếu thu cũ
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

--2 Chi tiết phiếu thu cũ
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

--3 Phiếu bảo hành
CREATE TABLE PhieuBaoHanh (
    maBH BIGINT AUTO_INCREMENT PRIMARY KEY,
    maSerial BIGINT NOT NULL,
    maNV BIGINT NOT NULL,
    ngayTiepNhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trangThaiBaoHanh BIT(1) NOT NULL DEFAULT b'0',--b'0' là ngoài bảo hành, b'1' là trong bảo hành
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
);
--============================================================================================================