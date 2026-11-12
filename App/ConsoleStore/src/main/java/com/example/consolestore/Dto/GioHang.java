package com.example.consolestore.Dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class GioHang implements Serializable {
    private final Map<Long, DongGioHang> dong = new LinkedHashMap<>();

    public Collection<DongGioHang> getDong() { return dong.values(); }
    public DongGioHang tim(Long maSP) { return dong.get(maSP); }
    public void them(DongGioHang d) { dong.put(d.getMaSP(), d); }
    public void xoa(Long maSP) { dong.remove(maSP); }
    public void xoaHet() { dong.clear(); }
    public boolean isEmpty() { return dong.isEmpty(); }

    public int getTongSoLuong() {
        return dong.values().stream().mapToInt(DongGioHang::getSoLuong).sum();
    }
    public BigDecimal getTongTien() {
        return dong.values().stream().map(DongGioHang::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}