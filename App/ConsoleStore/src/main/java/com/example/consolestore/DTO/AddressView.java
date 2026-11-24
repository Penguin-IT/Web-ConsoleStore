package com.example.consolestore.DTO;

import com.example.consolestore.Entity.DiaChiGiaoHang;

public record AddressView(
        Long id, String fullName, String phone,
        String province, String district, String ward, String street,
        String fullAddress, boolean defaultAddress) {

    public static AddressView from(DiaChiGiaoHang a) {
        return new AddressView(a.getId(), a.getFullName(), a.getPhone(),
                a.getProvince(), a.getDistrict(), a.getWard(), a.getStreet(),
                a.getFullAddress(), a.isDefaultAddress());
    }
}