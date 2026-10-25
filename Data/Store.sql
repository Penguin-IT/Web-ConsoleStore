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