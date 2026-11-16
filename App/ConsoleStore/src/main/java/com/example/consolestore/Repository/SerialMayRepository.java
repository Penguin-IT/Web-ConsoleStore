package com.example.consolestore.Repository;

import com.example.consolestore.Entity.SerialMay;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SerialMayRepository
        extends JpaRepository<SerialMay, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM SerialMay
                    WHERE maKho = :warehouseId
                      AND trangThai = N'TRONG_KHO'
                    ORDER BY maSP, maSerial
                    """,
            nativeQuery = true
    )
    List<SerialMay> available(
            @Param("warehouseId") Long warehouseId
    );

    @Query(
            value = """
                    SELECT *
                    FROM SerialMay WITH (UPDLOCK, HOLDLOCK)
                    WHERE maSerial IN (:ids)
                    ORDER BY maSerial
                    """,
            nativeQuery = true
    )
    List<SerialMay> lockIds(
            @Param("ids") Collection<Long> ids
    );
    @Query(
            value = """
            SELECT *
            FROM SerialMay WITH (UPDLOCK, HOLDLOCK)
            WHERE soSerial = :number
            """,
            nativeQuery = true
    )
    java.util.Optional<SerialMay> lockByNumber(
            @Param("number") String number
    );

    @Modifying
    @Query(
            value = """
            INSERT INTO SerialMay (
                maSP,
                maKho,
                soSerial,
                tinhTrang,
                trangThai
            )
            VALUES (
                :productId,
                :warehouseId,
                :number,
                N'CU',
                N'TRONG_KHO'
            )
            """,
            nativeQuery = true
    )
    void insertReceivedSerial(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("number") String number
    );

    @Modifying
    @Query(
            value = """
            UPDATE SerialMay
            SET maSP = :productId,
                maKho = :warehouseId,
                tinhTrang = N'CU',
                trangThai = N'TRONG_KHO'
            WHERE maSerial = :id
              AND trangThai = N'DA_BAN'
            """,
            nativeQuery = true
    )
    int receiveSoldSerial(
            @Param("id") Long id,
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );

    @Query(
            value = """
            SELECT COUNT_BIG(*)
            FROM PhieuBaoHanh
            WHERE maSerial = :id
              AND trangThai <> N'HOAN_TAT'
            """,
            nativeQuery = true
    )
    long activeWarranties(@Param("id") Long id);
    @Modifying
    @Query(
            value = """
            INSERT INTO SerialMay (
                maSP,
                maKho,
                soSerial,
                tinhTrang,
                trangThai
            )
            VALUES (
                :productId,
                :warehouseId,
                :number,
                :condition,
                N'TRONG_KHO'
            )
            """,
            nativeQuery = true
    )
    void insertPurchasedSerial(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("number") String number,
            @Param("condition") String condition
    );
}