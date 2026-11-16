package com.example.consolestore.Dto;

import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.HoaDonRepository;

import java.util.List;

public record PaymentReview(
        ThanhToan payment,
        HoaDon invoice,
        List<HoaDonRepository.LineInfo> lines
) {
}