package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "NhaCungCap")
public class NhaCungCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNCC")
    private Long id;

    @Nationalized
    @Column(name = "tenNCC", length = 100)
    private String name;

    @Nationalized
    @Column(name = "soDienThoai", length = 10)
    private String phone;

    @Nationalized
    @Column(name = "email", length = 100)
    private String email;

    @Nationalized
    @Column(name = "diaChi", length = 255)
    private String address;

    @Column(name = "ngayTao", columnDefinition = "datetime")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    protected NhaCungCap() {
    }

    public NhaCungCap(
            String name,
            String phone,
            String email,
            String address
    ) {
        updateDetails(name, phone, email, address);
        this.createdAt = LocalDateTime.now();
    }

    public void updateDetails(
            String name,
            String phone,
            String email,
            String address
    ) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
}