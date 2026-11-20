package com.example.consolestore.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GioHang")
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maGioHang")
    private Long id;

    @Column(name = "maKH")
    private Long customerId;

    @Column(name = "maKhach", length = 36)
    private String guestToken;

    @Column(
            name = "ngayCapNhat",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "datetime"
    )
    private LocalDateTime updatedAt;

    protected GioHang() {
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getGuestToken() { return guestToken; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}