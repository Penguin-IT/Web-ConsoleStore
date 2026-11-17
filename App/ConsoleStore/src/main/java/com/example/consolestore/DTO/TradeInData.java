package com.example.consolestore.DTO;

import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.PhieuThuCuRepository;

import java.util.List;

public final class TradeInData {

    private TradeInData() {
    }

    public record Options(
            List<Kho> warehouses,
            List<PhieuThuCuRepository.UsedProduct> products,
            KhachHang customer
    ) {
    }

    public record Detail(
            PhieuThuCu receipt,
            KhachHang customer,
            Kho warehouse,
            List<PhieuThuCuRepository.TradeLine> lines,
            String note,
            boolean cashPaid
    ) {
    }
}