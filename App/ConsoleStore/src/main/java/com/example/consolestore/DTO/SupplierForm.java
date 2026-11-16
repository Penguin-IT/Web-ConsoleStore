package com.example.consolestore.DTO;

import com.example.consolestore.Entity.NhaCungCap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

public class SupplierForm {

    private Long id;
    private String name = "";
    private String phone = "";
    private String email = "";
    private String address = "";
    private String version;
    private LocalDateTime createdAt;

    public static SupplierForm from(NhaCungCap supplier) {
        SupplierForm form = new SupplierForm();

        form.id = supplier.getId();
        form.name = supplier.getName();
        form.phone = Objects.toString(supplier.getPhone(), "");
        form.email = Objects.toString(supplier.getEmail(), "");
        form.address = Objects.toString(supplier.getAddress(), "");
        form.createdAt = supplier.getCreatedAt();
        form.version = versionOf(supplier);

        return form;
    }

    public static String versionOf(NhaCungCap supplier) {
        StringBuilder source = new StringBuilder();

        for (Object value : new Object[]{
                supplier.getId(),
                supplier.getName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress()
        }) {
            String text = Objects.toString(value, "");

            source.append(text.length())
                    .append(':')
                    .append(text);
        }

        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    source.toString().getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}