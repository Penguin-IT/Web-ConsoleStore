package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "SerialMay")
public class SerialMay {

    @Id
    @Column(name = "maSerial")
    private Long id;

    @Column(name = "maSP")
    private Long productId;

    @Column(name = "maKho")
    private Long warehouseId;

    @Nationalized
    @Column(name = "soSerial", length = 100)
    private String number;

    @Nationalized
    @Column(name = "tinhTrang", length = 20)
    private String condition;

    @Nationalized
    @Column(name = "trangThai", length = 20)
    private String status;

    protected SerialMay() {
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public String getCondition() {
        return condition;
    }

    public String getStatus() {
        return status;
    }
}