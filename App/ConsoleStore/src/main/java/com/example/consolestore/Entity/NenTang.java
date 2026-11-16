package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name="NenTang")
public class NenTang {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="maNenTang")
    private Long maNenTang;
    @Nationalized @Column(name="tenNenTang",nullable=false,length=100)
    private String tenNenTang;
    protected NenTang() {}
    public Long getId() { return maNenTang; }
    public String getName() { return tenNenTang; }
    public NenTang(String name) {
        this.tenNenTang = name;
    }

    public void rename(String name) {
        this.tenNenTang = name;
    }
}
