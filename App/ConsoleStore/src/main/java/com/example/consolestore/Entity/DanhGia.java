package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "DanhGia")
public class DanhGia {

    @Id
    @Column(name = "maDG")
    private Long id;

    @Column(name = "maSP")
    private Long productId;

    @Column(name = "maKH")
    private Long customerId;

    @Column(name = "soSao")
    private int stars;

    @Nationalized
    @Column(name = "noiDung", length = 1000)
    private String content;

    @Column(name = "ngayTao", columnDefinition = "datetime")
    private LocalDateTime createdAt;

    protected DanhGia() {
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getCustomerId() { return customerId; }
    public int getStars() { return stars; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}