package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuNhap")
public class PhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maPN")
    private Long id;

    @Column(name = "maNCC", updatable = false)
    private Long supplierId;

    @Column(name = "maNV", updatable = false)
    private Long employeeId;

    @Column(name = "maKho", updatable = false)
    private Long warehouseId;

    @Column(
            name = "ngayNhap",
            columnDefinition = "datetime",
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "tongTien",
            precision = 18,
            scale = 0,
            updatable = false
    )
    private BigDecimal totalAmount;

    @Nationalized
    @Column(name = "ghiChu", length = 255, updatable = false)
    private String note;

    protected PhieuNhap() {
    }

    public PhieuNhap(
            Long supplierId,
            Long employeeId,
            Long warehouseId,
            String note
    ) {
        this.supplierId = supplierId;
        this.employeeId = employeeId;
        this.warehouseId = warehouseId;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public Long getEmployeeId() {
        return employeeId;
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