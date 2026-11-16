package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "PhieuBaoHanh")
public class PhieuBaoHanh {

    @Id
    @Column(name = "maBH")
    private Long id;

    @Column(name = "maSerial")
    private Long serialId;

    @Column(name = "maNV")
    private Long employeeId;

    @Column(name = "ngayTiepNhan", columnDefinition = "datetime")
    private LocalDateTime receivedAt;

    @Column(name = "trangThaiBaoHanh")
    private boolean covered;

    @Column(name = "ngayHenTra", columnDefinition = "datetime")
    private LocalDateTime dueAt;

    @Nationalized
    @Column(name = "noiDung", length = 255)
    private String notes;

    @Nationalized
    @Column(name = "moTaLoi", length = 255)
    private String description;

    @Column(name = "chiPhi", precision = 18, scale = 0)
    private BigDecimal cost;

    @Nationalized
    @Column(name = "trangThai", length = 20)
    private String status;

    protected PhieuBaoHanh() {
    }

    public Long getId() {
        return id;
    }

    public Long getSerialId() {
        return serialId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public boolean isCovered() {
        return covered;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public String getNotes() {
        return notes;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getStatus() {
        return status;
    }
}