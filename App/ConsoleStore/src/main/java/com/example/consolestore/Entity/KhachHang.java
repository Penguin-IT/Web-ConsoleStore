package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "KhachHang")
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maKH")
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

    @Column(name = "ngayTao",nullable = false,columnDefinition = "datetime")
    private LocalDateTime createdAt;

    protected KhachHang() {
    }

    public KhachHang(String fullName, String phone, String email) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void updateProfile(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }
    public void updateDetails(String fullName, String phone, String email, String address) {
        updateProfile(fullName, phone, address);
        this.email = email;
    }
}