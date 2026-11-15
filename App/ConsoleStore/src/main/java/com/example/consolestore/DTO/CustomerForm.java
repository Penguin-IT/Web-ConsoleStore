package com.example.consolestore.DTO;

import com.example.consolestore.Entity.KhachHang;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

public class CustomerForm {

    private Long id;
    private String fullName = "";
    private String phone = "";
    private String email = "";
    private String address = "";
    private String version;
    private LocalDateTime createdAt;

    public static CustomerForm from(KhachHang customer) {
        CustomerForm form = new CustomerForm();

        form.id = customer.getId();
        form.fullName = customer.getFullName();
        form.phone = customer.getPhone();
        form.email = Objects.toString(customer.getEmail(), "");
        form.address = Objects.toString(customer.getAddress(), "");
        form.createdAt = customer.getCreatedAt();
        form.version = versionOf(customer);

        return form;
    }

    public static String versionOf(KhachHang customer) {
        StringBuilder source = new StringBuilder();

        for (Object value : new Object[]{
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress()
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
                                    source.toString()
                                            .getBytes(
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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