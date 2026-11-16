package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maTK")
    private Long id;

    @Nationalized
    @Column(name = "tenDangNhap", nullable = false, length = 100)
    private String username;

    @Nationalized
    @Column(name = "matKhau", nullable = false, length = 255)
    private String passwordHash;

    @Nationalized
    @Column(name = "vaiTro", nullable = false, length = 20)
    private String role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKH")
    private KhachHang customer;

    @Column(name = "biKhoa", nullable = false)
    private boolean locked;

    @Column(
            name = "ngayTao",
            nullable = false,
            columnDefinition = "datetime"
    )
    private LocalDateTime createdAt;

    protected TaiKhoan() {
    }

    public TaiKhoan(
            String username,
            String passwordHash,
            KhachHang customer
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.customer = customer;
        this.role = "KHACHHANG";
        this.locked = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public KhachHang getCustomer() {
        return customer;
    }

    public boolean isLocked() {
        return locked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNV")
    private NhanVien employee;

    public NhanVien getEmployee() {
        return employee;
    }

    public static TaiKhoan employeeAccount(
            String username,
            String passwordHash,
            String role,
            NhanVien employee
    ) {
        TaiKhoan account = new TaiKhoan();

        account.username = username;
        account.passwordHash = passwordHash;
        account.role = role;
        account.employee = employee;
        account.customer = null;
        account.locked = false;
        account.createdAt = LocalDateTime.now();

        return account;
    }

    public void updateAccess(String role, boolean locked) {
        this.role = role;
        this.locked = locked;
    }

    public void lock() {
        this.locked = true;
    }
}