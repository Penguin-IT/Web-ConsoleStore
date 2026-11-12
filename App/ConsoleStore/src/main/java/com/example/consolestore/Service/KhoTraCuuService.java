package com.example.consolestore.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class KhoTraCuuService {
    public static final long MA_LOAI_MAY = 1L;   // LoaiSanPham 'Máy console' trong Store.sql

    @PersistenceContext
    private EntityManager em;

    public int tonKho(long maKho, long maSP) {
        Object r = em.createNativeQuery(
                        "SELECT COALESCE(SUM(soLuong), 0) FROM TonKho WHERE maKho = :k AND maSP = :s")
                .setParameter("k", maKho).setParameter("s", maSP).getSingleResult();
        return ((Number) r).intValue();
    }

    /** Máy console phải bán theo serial, mỗi máy một dòng. */
    public boolean laMay(long maSP) {
        Object r = em.createNativeQuery(
                        "SELECT COUNT(*) FROM SanPham WHERE maSP = :s AND maLoai = :l")
                .setParameter("s", maSP).setParameter("l", MA_LOAI_MAY).getSingleResult();
        return ((Number) r).intValue() > 0;
    }
}