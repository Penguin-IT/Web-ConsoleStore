package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

@Entity
@Table(name = "DiaChiGiaoHang")
public class DiaChiGiaoHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maDC")
    private Long id;

    @Column(name = "maKH", nullable = false)
    private Long customerId;

    @Nationalized @Column(name = "tenNguoiNhan", nullable = false, length = 100)
    private String fullName;

    @Nationalized @Column(name = "soDienThoai", nullable = false, length = 10)
    private String phone;

    @Nationalized @Column(name = "tinhThanh", nullable = false, length = 40)
    private String province;

    @Nationalized @Column(name = "quanHuyen", nullable = false, length = 50)
    private String district;

    @Nationalized @Column(name = "phuongXa", nullable = false, length = 50)
    private String ward;

    @Nationalized @Column(name = "soNhaDuong", nullable = false, length = 100)
    private String street;

    @Nationalized @Column(name = "diaChiDayDu", nullable = false, length = 255)
    private String fullAddress;

    @Column(name = "macDinh", nullable = false)
    private boolean defaultAddress;

    @Column(name = "ngayTao", nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    protected DiaChiGiaoHang() {}

    public DiaChiGiaoHang(Long customerId) {
        this.customerId = customerId;
        this.createdAt = LocalDateTime.now();
    }

    public void fill(String fullName, String phone, String province,
                     String district, String ward, String street, String fullAddress) {
        this.fullName = fullName;
        this.phone = phone;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.street = street;
        this.fullAddress = fullAddress;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getProvince() { return province; }
    public String getDistrict() { return district; }
    public String getWard() { return ward; }
    public String getStreet() { return street; }
    public String getFullAddress() { return fullAddress; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { this.defaultAddress = value; }
}