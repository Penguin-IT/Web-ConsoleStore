package com.example.consolestore.Service;

import com.example.consolestore.DTO.RegisterRequest;
import com.example.consolestore.Entity.KhachHang;
import com.example.consolestore.Entity.TaiKhoan;
import com.example.consolestore.Repository.KhachHangRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

@Service
public class AuthService implements UserDetailsService {

    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private final BCryptPasswordEncoder passwordEncoder =new BCryptPasswordEncoder(12);

    public AuthService(KhachHangRepository khachHangRepository, TaiKhoanRepository taiKhoanRepository) {
        this.khachHangRepository = khachHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Transactional
    public boolean register(
            RegisterRequest request,
            BindingResult errors
    ) {
        normalize(request);
        validate(request, errors);

        if (errors.hasErrors()) {
            return false;
        }

        if (taiKhoanRepository.existsByUsernameIgnoreCase(
                request.getUsername()
        )) {
            errors.rejectValue(
                    "username",
                    "register.username.duplicate",
                    "Tên đăng nhập đã được sử dụng."
            );
        }

        if (khachHangRepository.existsByPhone(request.getPhone())) {
            errors.rejectValue(
                    "phone",
                    "register.phone.duplicate",
                    "Số điện thoại đã được sử dụng."
            );
        }

        if (errors.hasErrors()) {
            return false;
        }

        String passwordHash = passwordEncoder.encode(
                request.getPassword()
        );

        KhachHang customer = new KhachHang(
                request.getFullName(),
                request.getPhone(),
                request.getEmail()
        );

        khachHangRepository.saveAndFlush(customer);

        TaiKhoan account = new TaiKhoan(
                request.getUsername(),
                passwordHash,
                customer
        );

        taiKhoanRepository.saveAndFlush(account);

        return true;
    }

    private void normalize(RegisterRequest request) {
        request.setFullName(clean(request.getFullName()));
        request.setPhone(clean(request.getPhone()));

        request.setEmail(
                clean(request.getEmail()).toLowerCase(Locale.ROOT)
        );

        request.setUsername(
                clean(request.getUsername()).toLowerCase(Locale.ROOT)
        );

        // Không loại bỏ khoảng trắng trong mật khẩu.
        if (request.getPassword() == null) {
            request.setPassword("");
        }

        if (request.getConfirmPassword() == null) {
            request.setConfirmPassword("");
        }
    }

    private void validate(
            RegisterRequest request,
            BindingResult errors
    ) {
        if (request.getFullName().isBlank()
                || request.getFullName().length() > 100) {
            errors.rejectValue(
                    "fullName",
                    "register.fullName.invalid",
                    "Họ tên bắt buộc và không vượt quá 100 ký tự."
            );
        }

        if (!request.getPhone().matches("0[0-9]{9}")) {
            errors.rejectValue(
                    "phone",
                    "register.phone.invalid",
                    "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0."
            );
        }

        if (request.getEmail().length() > 100
                || !request.getEmail().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {
            errors.rejectValue(
                    "email",
                    "register.email.invalid",
                    "Nhập email hợp lệ, tối đa 100 ký tự."
            );
        }

        if (!request.getUsername().matches("[a-z0-9_]{3,30}")) {
            errors.rejectValue(
                    "username",
                    "register.username.invalid",
                    "Tên đăng nhập gồm 3–30 ký tự: chữ không dấu, số hoặc _."
            );
        }

        String password = request.getPassword();

        if (password.isBlank() || password.length() < 8) {
            errors.rejectValue(
                    "password",
                    "register.password.short",
                    "Mật khẩu phải có ít nhất 8 ký tự."
            );
        } else if (password.indexOf('\0') >= 0
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue(
                    "password",
                    "register.password.invalid",
                    "Mật khẩu không hợp lệ hoặc vượt quá giới hạn 72 byte."
            );
        }

        if (!password.equals(request.getConfirmPassword())) {
            errors.rejectValue(
                    "confirmPassword",
                    "register.confirmPassword.invalid",
                    "Mật khẩu xác nhận không khớp."
            );
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalizedUsername = clean(username)
                .toLowerCase(Locale.ROOT);

        if (normalizedUsername.isBlank()
                || normalizedUsername.length() > 100) {
            throw new UsernameNotFoundException(
                    "Account not found"
            );
        }

        TaiKhoan account = taiKhoanRepository
                .findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found"
                ));

        String role = account.getRole();

        if (role == null
                || !Set.of(
                "ADMIN",
                "NHANVIEN",
                "KHACHHANG"
        ).contains(role)) {
            throw new UsernameNotFoundException(
                    "Account role is invalid"
            );
        }
        TaiKhoanRepository.LoginState state =
                taiKhoanRepository.loginState(
                        account.getUsername()
                ).orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found"
                ));

        if (!state.isUsable()) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Account is unavailable"
            );
        }
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(role)
                .accountLocked(account.isLocked())
                .build();
    }
}