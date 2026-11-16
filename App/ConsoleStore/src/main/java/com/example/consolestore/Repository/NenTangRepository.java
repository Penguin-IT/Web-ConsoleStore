package com.example.consolestore.Repository;

import com.example.consolestore.Entity.NenTang;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NenTangRepository
        extends JpaRepository<NenTang, Long> {

    Optional<NenTang> findByTenNenTang(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from NenTang p
            where p.maNenTang = :id
            """)
    Optional<NenTang> lockById(
            @Param("id") Long id
    );
}