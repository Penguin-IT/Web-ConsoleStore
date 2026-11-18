package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "HoaDon")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maHD")
    private Long id;

    @Column(name = "maKH", updatable = false)
    private Long customerId;

    @Column(name = "maNV")
    private Long employeeId;

    @Column(name = "maKho")
    private Long warehouseId;

    @Column(
            name = "ngayLap",
            columnDefinition = "datetime",
            updatable = false
    )
    private LocalDateTime createdAt;

    @Nationalized
    @Column(name = "kenhBan", length = 20, updatable = false)
    private String channel;

    @Nationalized
    @Column(name = "trangThai", length = 20)
    private String status;

    @Column(
            name = "tongTien",
            precision = 18,
            scale = 0,
            updatable = false
    )
    private BigDecimal totalAmount;

    @Nationalized
    @Column(name = "tenNguoiNhan", length = 100, updatable = false)
    private String recipientName;

    @Nationalized
    @Column(name = "soDienThoaiNhan", length = 10, updatable = false)
    private String recipientPhone;

    @Nationalized
    @Column(name = "diaChi", length = 255, updatable = false)
    private String address;

    @Nationalized
    @Column(name = "ghiChu", length = 255, updatable = false)
    private String note;

    @Column(name = "maYeuCau", length = 36, updatable = false)
    private String requestId;

    protected HoaDon() {
    }

    public HoaDon(
            Long customerId,
            String requestId,
            String name,
            String phone,
            String address,
            String note
    ) {
        this.customerId = customerId;
        this.requestId = requestId;
        this.recipientName = name;
        this.recipientPhone = phone;
        this.address = address;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.channel = "ONLINE";
        this.status = "CHO_XAC_NHAN";
        this.totalAmount = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getAddress() {
        return address;
    }

    public String getNote() {
        return note;
    }
    public Long getEmployeeId() {
        return employeeId;
    }

    public String getChannel() {
        return channel;
    }

    public static HoaDon counter(
            Long customerId,
            Long employeeId,
            Long warehouseId,
            String name,
            String phone,
            String note,
            String requestId
    ) {
        HoaDon invoice = new HoaDon();

        invoice.customerId = customerId;
        invoice.employeeId = employeeId;
        invoice.warehouseId = warehouseId;
        invoice.recipientName = name;
        invoice.recipientPhone = phone;
        invoice.note = note;
        invoice.requestId = requestId;
        invoice.createdAt = LocalDateTime.now();
        invoice.channel = "QUAY";
        invoice.status = "CHO_XAC_NHAN";
        invoice.totalAmount = BigDecimal.ZERO;

        return invoice;
    }
}