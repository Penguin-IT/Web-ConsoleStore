package com.example.consolestore.Repository;

import com.example.consolestore.Entity.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NhanVienRepository extends JpaRepository<NhanVien,Long> {
    @Query("""
            select e
            from NhanVien e
            where lower(e.fullName) like :keyword escape '~'
               or e.phone like :keyword escape '~'
            """)
    Page<NhanVien> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<NhanVien> findByPhone(String phone);

    @Query(value = """
            SELECT *
            FROM NhanVien WITH (UPDLOCK, HOLDLOCK)
            WHERE maNV = :id
            """, nativeQuery = true)
    Optional<NhanVien> lockById(
            @Param("id") Long id
    );

}
