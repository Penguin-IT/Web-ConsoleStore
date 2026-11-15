package com.example.consolestore.Repository;

import com.example.consolestore.Entity.NhaCungCap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NhaCungCapRepository extends JpaRepository<NhaCungCap, Long> {
    List<NhaCungCap> findByTenNCCContainingIgnoreCase(String ten);

}