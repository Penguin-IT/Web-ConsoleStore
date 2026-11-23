package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StaffService {

    private static final Set<String> EMPLOYEE_ROLES = Set.of(
            "ADMIN", "NHANVIEN", "BANHANG", "KHO", "BAOHANH"
    );

    private final NhanVienRepository employees;
    private final TaiKhoanRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public StaffService(NhanVienRepository employees, TaiKhoanRepository accounts, PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    public record AccountRow(
            Long id,
            String username,
            String owner,
            String role,
            boolean locked
    ) {
    }

    public Page<NhanVien> list(
            String username,
            String keyword,
            int page
    ) {
        admin(username);

        return employees.search(
                pattern(keyword),
                PageRequest.of(
                        Math.max(0, page),
                        15,
                        Sort.by("id").ascending()
                )
        );
    }

    public Page<AccountRow> accountList(
            String username,
            String keyword,
            int page
    ) {
        admin(username);

        return accounts.searchAccounts(
                pattern(keyword),
                PageRequest.of(
                        Math.max(0, page),
                        15,
                        Sort.by("id").ascending()
                )
        ).map(account -> {
            String owner = "Quản trị hệ thống";

            if (account.getEmployee() != null) {
                owner = account.getEmployee().getFullName();
            } else if (account.getCustomer() != null) {
                owner = account.getCustomer().getFullName();
            }

            return new AccountRow(
                    account.getId(),
                    account.getUsername(),
                    owner,
                    account.getRole(),
                    account.isLocked()
            );
        });
    }

    public StaffForm form(String username, Long id) {
        admin(username);

        NhanVien employee = employees.findById(id)
                .orElseThrow(this::notFound);

        return new StaffForm(
                employee.getFullName(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getAddress(),
                employee.getPosition(),
                employee.isRetired(),
                employeeVersion(employee)
        );
    }

    public StaffAccountForm accountForm(
            String username,
            Long id
    ) {
        admin(username);

        TaiKhoan account = accounts.findAccount(id)
                .orElseThrow(this::notFound);

        return new StaffAccountForm(
                account.getEmployee() == null
                        ? null
                        : account.getEmployee().getId(),
                account.getUsername(),
                "",
                "",
                account.getRole(),
                account.isLocked(),
                accountVersion(account)
        );
    }

    public NhanVien employee(String username, Long id) {
        admin(username);

        return employees.findById(id)
                .orElseThrow(this::notFound);
    }

    @Transactional(timeout = 20)
    public Long save(
            String username,
            Long id,
            StaffForm form
    ) {
        TaiKhoanRepository.PaymentAdmin actor = begin(username);

        String fullName = required(
                form.fullName(),
                100,
                "Họ tên"
        );

        String phone = clean(form.phone());

        if (!phone.matches("0[0-9]{9}")) {
            throw invalid(
                    "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0."
            );
        }

        String email = optional(form.email(), 100, "Email");

        if (email != null
                && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw invalid("Email không hợp lệ.");
        }

        String address = optional(
                form.address(),
                255,
                "Địa chỉ"
        );

        String position = optional(
                form.position(),
                100,
                "Chức vụ"
        );

        if (form.retired() == null) {
            throw invalid("Hãy chọn trạng thái nhân viên.");
        }

        NhanVien employee = null;

        if (id != null) {
            employee = employees.lockById(id)
                    .orElseThrow(this::notFound);

            if (!Objects.equals(
                    employeeVersion(employee),
                    form.version()
            )) {
                throw invalid(
                        "Hồ sơ đã được cập nhật. "
                                + "Hãy mở lại trang chỉnh sửa."
                );
            }
        }

        NhanVien duplicate = employees.findByPhone(phone)
                .orElse(null);

        if (duplicate != null
                && !Objects.equals(duplicate.getId(), id)) {
            throw invalid(
                    "Số điện thoại đã thuộc nhân viên khác."
            );
        }

        if (Boolean.TRUE.equals(form.retired())
                && id != null
                && Objects.equals(actor.getEmployeeId(), id)) {
            throw invalid(
                    "Không thể tự chuyển mình sang nghỉ việc."
            );
        }

        if (employee == null) {
            if (Boolean.TRUE.equals(form.retired())) {
                throw invalid(
                        "Nhân viên mới phải ở trạng thái đang làm."
                );
            }

            employee = new NhanVien(
                    fullName,
                    phone,
                    email,
                    address,
                    position
            );
        } else {
            employee.updateDetails(
                    fullName,
                    phone,
                    email,
                    address,
                    position,
                    Boolean.TRUE.equals(form.retired())
            );
        }

        employees.saveAndFlush(employee);

        if (employee.isRetired()) {
            accounts.lockEmployeeAccounts(employee.getId())
                    .forEach(TaiKhoan::lock);

            accounts.flush();
        }

        return employee.getId();
    }

    @Transactional(timeout = 20)
    public Long createAccount(
            String username,
            StaffAccountForm form
    ) {
        begin(username);

        if (form.employeeId() == null) {
            throw invalid("Hãy chọn nhân viên.");
        }

        NhanVien employee = employees
                .lockById(form.employeeId())
                .orElseThrow(this::notFound);

        if (employee.isRetired()) {
            throw invalid(
                    "Không tạo tài khoản cho nhân viên đã nghỉ việc."
            );
        }

        if (!accounts.lockEmployeeAccounts(employee.getId()).isEmpty()) {
            throw invalid(
                    "Nhân viên đã có tài khoản. "
                            + "Hãy mở danh sách tài khoản để chỉnh sửa."
            );
        }

        String login = clean(form.username())
                .toLowerCase(Locale.ROOT);

        if (!login.matches("[a-z0-9_]{3,30}")) {
            throw invalid(
                    "Tên đăng nhập gồm 3–30 ký tự: "
                            + "chữ không dấu, số hoặc dấu _."
            );
        }

        if (accounts.existsByUsernameIgnoreCase(login)) {
            throw invalid("Tên đăng nhập đã được sử dụng.");
        }

        if (!EMPLOYEE_ROLES.contains(
                clean(form.role())
        )) {
            throw invalid("Vai trò nhân viên không hợp lệ.");
        }

        String password = form.password();

        if (password == null
                || password.isBlank()
                || password.length() < 8
                || password.indexOf('\0') >= 0
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid(
                    "Mật khẩu tối thiểu 8 ký tự, tối đa 72 byte."
            );
        }

        if (!password.equals(form.confirmPassword())) {
            throw invalid("Mật khẩu xác nhận không khớp.");
        }

        TaiKhoan account = TaiKhoan.employeeAccount(
                login,
                passwordEncoder.encode(password),
                clean(form.role()),
                employee
        );

        return accounts.saveAndFlush(account).getId();
    }

    @Transactional(timeout = 20)
    public void updateAccount(
            String username,
            Long id,
            StaffAccountForm form
    ) {
        TaiKhoanRepository.PaymentAdmin actor = begin(username);

        TaiKhoan account = accounts.lockAccount(id)
                .orElseThrow(this::notFound);

        if (!Objects.equals(
                accountVersion(account),
                form.version()
        )) {
            throw invalid(
                    "Tài khoản đã được cập nhật. "
                            + "Hãy mở lại trang chỉnh sửa."
            );
        }

        if (form.locked() == null) {
            throw invalid("Hãy chọn trạng thái tài khoản.");
        }

        String role = clean(form.role());

        if (account.getCustomer() != null) {
            if (!"KHACHHANG".equals(role)) {
                throw invalid(
                        "Tài khoản khách hàng phải giữ quyền KHACHHANG."
                );
            }
        } else if (account.getEmployee() != null) {
            if (!EMPLOYEE_ROLES.contains(role)) {
                throw invalid(
                        "Vai trò nhân viên không hợp lệ."
                );
            }

            if (account.getEmployee().isRetired()
                    && !form.locked()) {
                throw invalid(
                        "Nhân viên đã nghỉ việc. "
                                + "Hãy cập nhật hồ sơ trước khi mở khóa."
                );
            }
        } else if (!"ADMIN".equals(role)) {
            throw invalid(
                    "Tài khoản quản trị hệ thống phải giữ quyền ADMIN."
            );
        }

        if (Objects.equals(actor.getAccountId(), id)
                && (form.locked() || !"ADMIN".equals(role))) {
            throw invalid(
                    "Không thể tự khóa hoặc hạ quyền tài khoản đang dùng."
            );
        }

        account.updateAccess(role, form.locked());

        accounts.flush();
    }

    private TaiKhoanRepository.PaymentAdmin begin(
            String username
    ) {
        accounts.lockAdministrators();

        return admin(username);
    }

    private TaiKhoanRepository.PaymentAdmin admin(
            String username
    ) {
        if (username == null) {
            throw new AccessDeniedException(
                    "Bạn không có quyền quản lý nhân viên."
            );
        }

        return accounts.paymentAdmin(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "Bạn không có quyền quản lý nhân viên."
                ));
    }

    private String employeeVersion(NhanVien employee) {
        return token(
                employee.getId(),
                employee.getFullName(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getAddress(),
                employee.getPosition(),
                employee.isRetired()
        );
    }

    private String accountVersion(TaiKhoan account) {
        return token(
                account.getId(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getRole(),
                account.isLocked(),
                account.getEmployee() == null
                        ? null
                        : account.getEmployee().getId(),
                account.getCustomer() == null
                        ? null
                        : account.getCustomer().getId()
        );
    }

    private String token(Object... values) {
        StringBuilder content = new StringBuilder();

        for (Object value : values) {
            String text = value == null ? "" : value.toString();

            content.append(text.length())
                    .append(':')
                    .append(text);
        }

        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(content.toString().getBytes(
                                    StandardCharsets.UTF_8
                            ))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String pattern(String keyword) {
        String value = clean(keyword)
                .toLowerCase(Locale.ROOT);

        if (value.length() > 100) {
            throw invalid("Từ khóa tối đa 100 ký tự.");
        }

        return "%"
                + value.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";
    }

    private String required(
            String value,
            int max,
            String label
    ) {
        String result = clean(value);

        if (result.isBlank() || result.length() > max) {
            throw invalid(
                    label + " bắt buộc, tối đa " + max + " ký tự."
            );
        }

        return result;
    }

    private String optional(
            String value,
            int max,
            String label
    ) {
        String result = clean(value);

        if (result.length() > max) {
            throw invalid(
                    label + " tối đa " + max + " ký tự."
            );
        }

        return result.isBlank() ? null : result;
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy nhân viên hoặc tài khoản."
        );
    }
}
