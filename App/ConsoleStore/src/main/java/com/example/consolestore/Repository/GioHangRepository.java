package com.example.consolestore.Repository;

import com.example.consolestore.Entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GioHangRepository extends JpaRepository<GioHang, Long> {

    interface Line {
        Long getProductId();
        Integer getQuantity();
    }

    Optional<GioHang> findByCustomerId(Long customerId);

    Optional<GioHang> findByGuestToken(String guestToken);

    @Query(
            value = """
                SELECT *
                FROM GioHang WITH (UPDLOCK, HOLDLOCK)
                WHERE maGioHang = :cartId
                """,
            nativeQuery = true
    )
    Optional<GioHang> lockCart(@Param("cartId") Long cartId);

    // Tạo giỏ nếu chưa có (an toàn khi 2 request cùng lúc)
    @Modifying
    @Query(
            value = """
                INSERT INTO GioHang (maKH)
                SELECT :customerId
                WHERE NOT EXISTS (
                    SELECT 1 FROM GioHang WITH (UPDLOCK, HOLDLOCK)
                    WHERE maKH = :customerId
                )
                """,
            nativeQuery = true
    )
    int createForCustomer(@Param("customerId") Long customerId);

    @Modifying
    @Query(
            value = """
                INSERT INTO GioHang (maKhach)
                SELECT :token
                WHERE NOT EXISTS (
                    SELECT 1 FROM GioHang WITH (UPDLOCK, HOLDLOCK)
                    WHERE maKhach = :token
                )
                """,
            nativeQuery = true
    )
    int createForGuest(@Param("token") String token);

    @Query(
            value = """
                SELECT maSP AS productId, soLuong AS quantity
                FROM ChiTietGioHang
                WHERE maGioHang = :cartId
                ORDER BY thuTu, maSP
                """,
            nativeQuery = true
    )
    List<Line> lines(@Param("cartId") Long cartId);

    @Modifying
    @Query(
            value = "DELETE FROM ChiTietGioHang WHERE maGioHang = :cartId",
            nativeQuery = true
    )
    int clearLines(@Param("cartId") Long cartId);

    @Modifying
    @Query(
            value = """
                INSERT INTO ChiTietGioHang (maGioHang, maSP, soLuong, thuTu)
                VALUES (:cartId, :productId, :quantity, :position)
                """,
            nativeQuery = true
    )
    int addLine(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity,
            @Param("position") int position
    );

    @Modifying
    @Query(
            value = """
                UPDATE GioHang
                SET ngayCapNhat = CURRENT_TIMESTAMP
                WHERE maGioHang = :cartId
                """,
            nativeQuery = true
    )
    int touch(@Param("cartId") Long cartId);

    @Modifying
    @Query(
            value = "DELETE FROM GioHang WHERE maGioHang = :cartId",
            nativeQuery = true
    )
    int deleteCart(@Param("cartId") Long cartId);

    @Modifying
    @Query(
            value = """
                DELETE FROM GioHang
                WHERE maKhach IS NOT NULL AND ngayCapNhat < :before
                """,
            nativeQuery = true
    )
    int deleteStaleGuestCarts(@Param("before") LocalDateTime before);
}