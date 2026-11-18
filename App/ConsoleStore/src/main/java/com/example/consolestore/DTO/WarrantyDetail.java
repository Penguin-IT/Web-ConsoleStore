package com.example.consolestore.DTO;

import com.example.consolestore.Entity.PhieuBaoHanh;
import com.example.consolestore.Repository.PhieuBaoHanhRepository;

public record WarrantyDetail(
        PhieuBaoHanh receipt,
        PhieuBaoHanhRepository.Lookup lookup,
        String notes
) {
}