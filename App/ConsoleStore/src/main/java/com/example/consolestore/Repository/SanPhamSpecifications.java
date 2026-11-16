package com.example.consolestore.Repository;

import com.example.consolestore.DTO.CatalogSearch;
import com.example.consolestore.Entity.SanPham;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;

public final class SanPhamSpecifications {
    private SanPhamSpecifications() {}
    public static Specification<SanPham> search(CatalogSearch f) {
        return (root,query,cb)->{
            List<Predicate> conditions=new ArrayList<>();
            conditions.add(cb.isFalse(root.get("deleted")));
            if(!f.getQ().isBlank()) {
                String literal=f.getQ().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[");
                conditions.add(cb.like(cb.lower(root.get("tenSP")),"%"+literal+"%",'\\'));
            }
            if(f.getCategoryId()!=null) conditions.add(cb.equal(root.get("loaiSanPham").get("maLoai"),f.getCategoryId()));
            else if(!f.getCategory().isBlank()) conditions.add(cb.equal(root.get("loaiSanPham").get("tenLoai"),f.getCategory()));
            if(f.getPlatformId()!=null) conditions.add(cb.equal(root.get("nenTang").get("maNenTang"),f.getPlatformId()));
            else if(!f.getPlatform().isBlank()) conditions.add(cb.equal(root.get("nenTang").get("tenNenTang"),f.getPlatform()));
            if(!f.getCondition().isBlank()) conditions.add(cb.equal(root.get("tinhTrang"),f.getCondition()));
            if(f.getMinPrice()!=null)conditions.add(cb.greaterThanOrEqualTo(root.get("giaBan"),f.getMinPrice()));
            if(f.getMaxPrice()!=null)conditions.add(cb.lessThanOrEqualTo(root.get("giaBan"),f.getMaxPrice()));
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
