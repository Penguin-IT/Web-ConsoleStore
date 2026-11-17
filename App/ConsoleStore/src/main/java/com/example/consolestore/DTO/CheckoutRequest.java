package com.example.consolestore.DTO;

public class CheckoutRequest {

    private String fullName = "";
    private String phone = "";
    private String address = "";
    private String note = "";
    private String paymentMethod = "CHUYEN_KHOAN";

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) {
        fullName = value;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String value) {
        phone = value;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        address = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        note = value;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String value) {
        paymentMethod = value;
    }
}