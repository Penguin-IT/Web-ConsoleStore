package com.example.consolestore.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Kho")
public class Kho {

    @Id
    @Column(name = "maKho")
    private Long id;

    @Nationalized
    @Column(name = "tenKho", length = 100)
    private String name;

    protected Kho() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}