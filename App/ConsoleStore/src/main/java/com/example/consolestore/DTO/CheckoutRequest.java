package com.example.consolestore.DTO;

public class CheckoutRequest {

    private String fullName = "";
    private String phone = "";
    private boolean saveAddress;
    private boolean makeDefaultAddress;

    public boolean isSaveAddress() { return saveAddress; }
    public void setSaveAddress(boolean v) { saveAddress = v; }
    public boolean isMakeDefaultAddress() { return makeDefaultAddress; }
    public void setMakeDefaultAddress(boolean v) { makeDefaultAddress = v; }
    // Địa chỉ giao hàng nhập theo từng cấp.
    private String province = "";
    private String district = "";
    private String ward = "";
    private String street = "";

    // Chuỗi địa chỉ đầy đủ, được CheckoutValidator ghép từ các trường trên
    // và lưu vào cột HoaDon.diaChi. Không nhận trực tiếp từ người dùng.
    private String address = "";

    private String note = "";
    private String couponCode = "";

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String value) {
        couponCode = value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
    }
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

    public String getProvince() {
        return province;
    }

    public void setProvince(String value) {
        province = value;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String value) {
        district = value;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String value) {
        ward = value;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String value) {
        street = value;
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
