package com.example.consolestore.Repository;

import com.example.consolestore.Entity.DiaChiGiaoHang;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface DiaChiGiaoHangRepository extends JpaRepository<DiaChiGiaoHang, Long> {

    List<DiaChiGiaoHang> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(Long customerId);

    Optional<DiaChiGiaoHang> findByIdAndCustomerId(Long id, Long customerId);

    Optional<DiaChiGiaoHang> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByCustomerId(Long customerId);

    boolean existsByCustomerIdAndPhoneAndFullAddress(Long customerId, String phone, String fullAddress);

    // Phải gọi TRƯỚC khi load entity (vì clearAutomatically làm detach mọi entity đang quản lý)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update DiaChiGiaoHang a set a.defaultAddress = false "
            + "where a.customerId = :cid and a.defaultAddress = true")
    void clearDefault(@Param("cid") Long customerId);
}