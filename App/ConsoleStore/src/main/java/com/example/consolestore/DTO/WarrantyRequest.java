package com.example.consolestore.DTO;


import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class WarrantyRequest {

    @NotBlank(message = "Token không được để trống")
    private String token;
    @NotBlank(message = "Serial không được để trống")
    private String serial;
    private String description;
    private String notes;

    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "Cost phải là số không âm")
    private String cost = "0";
    @NotBlank(message = "Action không được để trống")
    @Pattern(regexp = "^(save|submit|approve|reject)$", message = "Action không hợp lệ")
    private String action = "save";
    private String expectedStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Future(message = "dueAt không được ở quá khứ")
    private LocalDateTime dueAt;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
}