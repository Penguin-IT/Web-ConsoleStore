package com.example.consolestore.Entity;

import jakarta.persistence.*;

@Entity
@Table(name="TonKho")
public class TonKho {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="maTon") private Long maTon;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="maSP") private SanPham sanPham;
    @Column(name="maKho",nullable=false) private Long maKho;
    @Column(name="soLuong",nullable=false) private int soLuong;
    protected TonKho() {}
}
