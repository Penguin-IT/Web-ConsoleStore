package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name="LoaiSanPham")
public class LoaiSanPham {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="maLoai")
    private Long maLoai;
    @Nationalized @Column(name="tenLoai",nullable=false,length=100)
    private String tenLoai;
    protected LoaiSanPham() {}
    public Long getId() { return maLoai; }
    public String getName() { return tenLoai; }
    public LoaiSanPham(String name) {
        this.tenLoai = name;
    }

    public void rename(String name) {
        this.tenLoai = name;
    }
}
