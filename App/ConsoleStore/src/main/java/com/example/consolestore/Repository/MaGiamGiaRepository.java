package com.example.consolestore.Repository;

import com.example.consolestore.Entity.MaGiamGia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, String> {

    /** Khóa dòng mã khi đặt hàng để không bị dùng quá số lượng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MaGiamGia c where c.code = :code")
    Optional<MaGiamGia> lockByCode(@Param("code") String code);

    @Query("""
            select c from MaGiamGia c
            where c.active = true and c.featured = true
              and c.startsAt <= :now and c.endsAt >= :now
            order by c.endsAt
            """)
    List<MaGiamGia> featured(@Param("now") LocalDateTime now);
}