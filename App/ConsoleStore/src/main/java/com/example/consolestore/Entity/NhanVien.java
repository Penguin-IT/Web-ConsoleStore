package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "NhanVien")
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNV")
    private Long id;

    @Nationalized
    @Column(name = "hoTen", nullable = false, length = 100)
    private String fullName;

    @Nationalized
    @Column(name = "soDienThoai", nullable = false, length = 10)
    private String phone;

    @Nationalized
    @Column(name = "email", length = 100)
    private String email;

    @Nationalized
    @Column(name = "diaChi", length = 255)
    private String address;

    @Nationalized
    @Column(name = "chucVu", length = 100)
    private String position;

    @Column(name = "ngayVaoLam",nullable = false, columnDefinition = "datetime")
    private LocalDateTime startedAt;

    @Column(name = "daNghiViec", nullable = false)
    private boolean retired;

    protected NhanVien() {
    }

    public NhanVien(String fullName, String phone, String email, String address, String position) {
        updateDetails(
                fullName,
                phone,
                email,
                address,
                position,
                false
        );

        this.startedAt = LocalDateTime.now();
    }

    public void updateDetails(
            String fullName,
            String phone,
            String email,
            String address,
            String position,
            boolean retired
    ) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.position = position;
        this.retired = retired;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getPosition() {
        return position;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public boolean isRetired() {
        return retired;
    }
}