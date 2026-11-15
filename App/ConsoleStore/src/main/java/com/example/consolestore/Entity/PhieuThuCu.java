package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuThuCu")
public class PhieuThuCu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maPTC")
    private Long id;

    @Column(name = "maKH", updatable = false)
    private Long customerId;

    @Column(name = "maNV", updatable = false)
    private Long employeeId;

    @Column(name = "maHD")
    private Long invoiceId;

    @Column(name = "maKho", updatable = false)
    private Long warehouseId;

    @Column(
            name = "ngayThu",
            updatable = false,
            columnDefinition = "datetime"
    )
    private LocalDateTime createdAt;

    @Column(
            name = "tongTienThu",
            precision = 18,
            scale = 0,
            updatable = false
    )
    private BigDecimal totalAmount;

    @Nationalized
    @Column(name = "ghiChu", length = 255, updatable = false)
    private String note;

    protected PhieuThuCu() {
    }

    public PhieuThuCu(
            Long customerId,
            Long employeeId,
            Long warehouseId,
            String note
    ) {
        this.customerId = customerId;
        this.employeeId = employeeId;
        this.warehouseId = warehouseId;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNote() {
        return note;
    }
}