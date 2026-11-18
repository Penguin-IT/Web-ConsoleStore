package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThanhToan")
public class ThanhToan {

    @Id
    @Column(name = "maTT")
    private Long id;

    @Column(name = "maHD")
    private Long invoiceId;

    @Nationalized
    @Column(name = "phuongThuc", length = 20)
    private String method;

    @Column(name = "soTien", precision = 18, scale = 0)
    private BigDecimal amount;

    @Nationalized
    @Column(name = "trangThai", length = 20)
    private String status;

    @Column(name = "ngayThanhToan", columnDefinition = "datetime")
    private LocalDateTime paidAt;

    protected ThanhToan() {
    }

    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}