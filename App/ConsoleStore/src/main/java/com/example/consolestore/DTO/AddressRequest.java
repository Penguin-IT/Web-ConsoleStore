package com.example.consolestore.DTO;

public class AddressRequest {
    private String fullName = "";
    private String phone = "";
    private String province = "";
    private String district = "";
    private String ward = "";
    private String street = "";
    private boolean makeDefault;

    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getProvince() { return province; }
    public void setProvince(String v) { province = v; }
    public String getDistrict() { return district; }
    public void setDistrict(String v) { district = v; }
    public String getWard() { return ward; }
    public void setWard(String v) { ward = v; }
    public String getStreet() { return street; }
    public void setStreet(String v) { street = v; }
    public boolean isMakeDefault() { return makeDefault; }
    public void setMakeDefault(boolean v) { makeDefault = v; }
}