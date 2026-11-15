package com.example.consolestore.Repository;

import com.example.consolestore.Entity.TaiKhoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan,Long> {

    boolean existsByUsernameIgnoreCase(String username);
    Optional<TaiKhoan> findByUsernameIgnoreCase(String username);
    @org.springframework.data.jpa.repository.Query("""
        select t
        from TaiKhoan t
        left join fetch t.customer
        where lower(t.username) = lower(:username)
        """)
    java.util.Optional<com.example.consolestore.Entity.TaiKhoan>
    findWithCustomer(
            @org.springframework.data.repository.query.Param("username")
            String username
    );
    @org.springframework.data.jpa.repository.Query(
            value = """
                SELECT t.*
                FROM TaiKhoan t WITH (UPDLOCK, HOLDLOCK)
                WHERE LOWER(t.tenDangNhap) = LOWER(:username)
                """,
            nativeQuery = true
    )
    Optional<TaiKhoan> lockByUsername(
            @org.springframework.data.repository.query.Param("username")
            String username
    );
    interface PosStaff {
        Long getEmployeeId();
        String getRole();
    }

    @org.springframework.data.jpa.repository.Query(
            value = """
                SELECT
                    t.maNV AS employeeId,
                    t.vaiTro AS role
                FROM TaiKhoan t
                JOIN NhanVien n ON n.maNV = t.maNV
                WHERE LOWER(t.tenDangNhap) = LOWER(:username)
                  AND t.biKhoa = 0
                  AND n.daNghiViec = 0
                  AND t.vaiTro IN (N'ADMIN', N'NHANVIEN')
                """,
            nativeQuery = true
    )
    Optional<PosStaff> posStaff(
            @org.springframework.data.repository.query.Param("username")
            String username
    );
    interface PaymentAdmin {
        Long getAccountId();
        Long getEmployeeId();
    }

    @org.springframework.data.jpa.repository.Query(
            value = """
            SELECT
                t.maTK AS accountId,
                t.maNV AS employeeId
            FROM TaiKhoan t
            LEFT JOIN NhanVien n ON n.maNV = t.maNV
            WHERE LOWER(t.tenDangNhap) = LOWER(:username)
              AND t.vaiTro = N'ADMIN'
              AND t.biKhoa = 0
              AND (
                  t.maNV IS NULL
                  OR n.daNghiViec = 0
              )
            """,
            nativeQuery = true
    )
    Optional<PaymentAdmin> paymentAdmin(
            @org.springframework.data.repository.query.Param("username")
            String username
    );
    @EntityGraph(attributePaths = {
            "employee",
            "customer"
    })
    @Query("""
        select a
        from TaiKhoan a
        where lower(a.username) like :keyword escape '~'
        """)
    Page<TaiKhoan> searchAccounts(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"employee", "customer"})
    @Query("""
        select a
        from TaiKhoan a
        where a.id = :id
        """)
    Optional<TaiKhoan> findAccount(
            @Param("id") Long id
    );

    @Query(value = """
        SELECT *
        FROM TaiKhoan WITH (UPDLOCK, HOLDLOCK)
        WHERE maTK = :id
        """, nativeQuery = true)
    Optional<TaiKhoan> lockAccount(
            @Param("id") Long id
    );

    @Query(value = """
        SELECT *
        FROM TaiKhoan WITH (UPDLOCK, HOLDLOCK)
        WHERE vaiTro = N'ADMIN'
        ORDER BY maTK
        """, nativeQuery = true)
    List<TaiKhoan> lockAdministrators();

    @Query(value = """
        SELECT *
        FROM TaiKhoan WITH (UPDLOCK, HOLDLOCK)
        WHERE maNV = :employeeId
        ORDER BY maTK
        """, nativeQuery = true)
    List<TaiKhoan> lockEmployeeAccounts(@Param("employeeId") Long employeeId);

    interface LoginState {

        Long getAccountId();

        String getRole();

        Boolean getLocked();

        Long getEmployeeId();

        Long getCustomerId();

        Boolean getRetired();

        default boolean isUsable() {
            if (!Boolean.FALSE.equals(getLocked())
                    || Boolean.TRUE.equals(getRetired())
                    || getRole() == null) {
                return false;
            }

            return switch (getRole()) {
                case "ADMIN" -> getCustomerId() == null;
                case "NHANVIEN" ->
                        getEmployeeId() != null
                                && getCustomerId() == null;
                case "KHACHHANG" ->
                        getCustomerId() != null
                                && getEmployeeId() == null;
                default -> false;
            };
        }
    }

    @Query(value = """
        SELECT
            a.maTK AS accountId,
            a.vaiTro AS role,
            a.biKhoa AS locked,
            a.maNV AS employeeId,
            a.maKH AS customerId,
            CAST(COALESCE(e.daNghiViec, 0) AS BIT) AS retired
        FROM TaiKhoan a
        LEFT JOIN NhanVien e ON e.maNV = a.maNV
        WHERE LOWER(a.tenDangNhap) = LOWER(:username)
        """, nativeQuery = true)
    Optional<LoginState> loginState(@Param("username") String username);
}
