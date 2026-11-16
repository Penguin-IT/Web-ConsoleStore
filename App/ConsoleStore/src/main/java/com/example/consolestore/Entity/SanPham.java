package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;

@Entity
@Table(name="SanPham")
public class SanPham {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="maSP") private Long maSP;
    @Nationalized @Column(name="tenSP",nullable=false,length=100) private String tenSP;
    @Nationalized @Column(name="moTa",length=255) private String moTa;
    @Column(name="giaBan",precision=18,scale=0,nullable=false) private BigDecimal giaBan;
    @Nationalized @Column(name="hinhAnh",length=255) private String hinhAnh;
    @Column(name="deleted",nullable=false) private boolean deleted;
    @Nationalized @Column(name="tinhTrang",nullable=false,length=3) private String tinhTrang;
    @Column(name="maSPGoc") private Long maSPGoc;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="maLoai") private LoaiSanPham loaiSanPham;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="maNenTang") private NenTang nenTang;
    protected SanPham() {}
    public Long getMaSP(){return maSP;}
    public String getTenSP(){return tenSP;}
    public String getMoTa(){return moTa;}
    public BigDecimal getGiaBan(){return giaBan;}
    public String getHinhAnh(){return hinhAnh;}
    public boolean isDeleted(){return deleted;}
    public String getTinhTrang(){return tinhTrang;}
    public Long getMaSPGoc(){return maSPGoc;}
    public LoaiSanPham getLoaiSanPham(){return loaiSanPham;}
    public NenTang getNenTang(){return nenTang;}
    public SanPham(
            String name,
            String description,
            BigDecimal price,
            String image,
            String condition,
            Long baseProductId,
            LoaiSanPham category,
            NenTang platform
    ) {
        updateCatalog(
                name,
                description,
                price,
                image,
                condition,
                baseProductId,
                category,
                platform
        );

        this.deleted = false;
    }

    public void updateCatalog(
            String name,
            String description,
            BigDecimal price,
            String image,
            String condition,
            Long baseProductId,
            LoaiSanPham category,
            NenTang platform
    ) {
        this.tenSP = name;
        this.moTa = description;
        this.giaBan = price;
        this.hinhAnh = image;
        this.tinhTrang = condition;
        this.maSPGoc = baseProductId;
        this.loaiSanPham = category;
        this.nenTang = platform;
    }

    public void markDeleted() {
        this.deleted = true;
    }
}
