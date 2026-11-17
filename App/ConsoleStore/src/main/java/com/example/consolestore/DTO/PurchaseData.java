package com.example.consolestore.DTO;

import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.PhieuNhapRepository;

import java.util.*;

public final class PurchaseData {

    private PurchaseData() {
    }

    public record Options(
            List<NhaCungCap> suppliers,
            List<Kho> warehouses,
            List<PhieuNhapRepository.ProductOption> products
    ) {
    }

    public record Detail(
            PhieuNhapRepository.ReceiptRow receipt,
            List<PhieuNhapRepository.LineRow> lines,
            Map<Long, List<String>> serials,
            String note
    ) {
    }
}