package com.example.consolestore.Service;

import com.example.consolestore.Dto.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.nio.charset.StandardCharsets;

@Service
public class AccountService {

    private final TaiKhoanRepository accounts;
    private final KhachHangRepository customers;
    private final PasswordEncoder encoder;

    public AccountService(TaiKhoanRepository accounts, KhachHangRepository customers, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.customers = customers;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public AccountProfile profile(String username) {
        TaiKhoan account = requireCustomer(
                accounts.findWithCustomer(username)
                        .orElseThrow(
                                () -> new AccessDeniedException(
                                        "Account is missing"
                                )
                        )
        );

        KhachHang customer = account.getCustomer();

        return new AccountProfile(
                account.getUsername(),
                clean(customer.getEmail()),
                clean(customer.getFullName()),
                clean(customer.getPhone()),
                clean(customer.getAddress())
        );
    }

    @Transactional(timeout = 20)
    public boolean updateProfile(
            String username,
            ProfileRequest form,
            BindingResult errors
    ) {
        TaiKhoan account = lockAccount(username);

        form.setFullName(clean(form.getFullName()));
        form.setPhone(clean(form.getPhone()));
        form.setAddress(clean(form.getAddress()));

        if (form.getFullName().isEmpty()
                || form.getFullName().length() > 100) {
            errors.rejectValue(
                    "fullName",
                    "profile.name.invalid",
                    "Họ tên bắt buộc, tối đa 100 ký tự."
            );
        }

        if (!form.getPhone().matches("0[0-9]{9}")) {
            errors.rejectValue(
                    "phone",
                    "profile.phone.invalid",
                    "Số điện thoại gồm 10 chữ số "
                            + "và bắt đầu bằng 0."
            );
        }

        if (form.getAddress().isEmpty()
                || form.getAddress().length() > 255) {
            errors.rejectValue(
                    "address",
                    "profile.address.invalid",
                    "Địa chỉ bắt buộc, tối đa 255 ký tự."
            );
        }

        if (errors.hasErrors()) {
            return false;
        }

        Long customerId = account.getCustomer().getId();

        KhachHang customer = customers.lockById(customerId)
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "Customer is missing"
                        )
                );

        if (customers.existsByPhoneAndIdNot(
                form.getPhone(),
                customerId
        )) {
            errors.rejectValue(
                    "phone",
                    "profile.phone.duplicate",
                    "Số điện thoại đã được khách hàng khác sử dụng."
            );

            return false;
        }

        customer.updateProfile(
                form.getFullName(),
                form.getPhone(),
                form.getAddress()
        );

        customers.saveAndFlush(customer);

        return true;
    }

    @Transactional(timeout = 20)
    public boolean changePassword(
            String username,
            PasswordChangeRequest form,
            BindingResult errors
    ) {
        TaiKhoan account = lockAccount(username);

        // Khoảng trắng là một phần của mật khẩu.
        String current = raw(form.getCurrentPassword());
        String next = raw(form.getNewPassword());
        String confirm = raw(form.getConfirmPassword());

        if (current.isBlank()
                || !validPasswordBytes(current)) {
            errors.rejectValue(
                    "currentPassword",
                    "password.current.invalid",
                    "Nhập mật khẩu hiện tại hợp lệ, tối đa 72 byte."
            );
        }

        if (next.isBlank()
                || next.length() < 8
                || !validPasswordBytes(next)) {
            errors.rejectValue(
                    "newPassword",
                    "password.new.invalid",
                    "Mật khẩu mới tối thiểu 8 ký tự, tối đa 72 byte."
            );
        }

        if (!next.equals(confirm)) {
            errors.rejectValue(
                    "confirmPassword",
                    "password.confirm.invalid",
                    "Mật khẩu xác nhận không khớp."
            );
        }

        if (errors.hasErrors()) {
            return false;
        }

        if (!encoder.matches(
                current,
                account.getPasswordHash()
        )) {
            errors.rejectValue(
                    "currentPassword",
                    "password.current.wrong",
                    "Mật khẩu hiện tại không đúng."
            );

            return false;
        }

        if (next.equals(current)) {
            errors.rejectValue(
                    "newPassword",
                    "password.new.same",
                    "Mật khẩu mới phải khác mật khẩu hiện tại."
            );

            return false;
        }

        account.changePasswordHash(
                encoder.encode(next)
        );

        accounts.saveAndFlush(account);

        return true;
    }

    private TaiKhoan lockAccount(String username) {
        return requireCustomer(
                accounts.lockByUsername(username)
                        .orElseThrow(
                                () -> new AccessDeniedException(
                                        "Account is missing"
                                )
                        )
        );
    }

    private TaiKhoan requireCustomer(
            TaiKhoan account
    ) {
        if (account.isLocked()
                || !"KHACHHANG".equals(account.getRole())
                || account.getCustomer() == null) {
            throw new AccessDeniedException(
                    "Customer access required"
            );
        }

        return account;
    }

    private static boolean validPasswordBytes(
            String value
    ) {
        return value.indexOf('\0') < 0
                && value.getBytes(
                StandardCharsets.UTF_8
        ).length <= 72;
    }

    private static String clean(String value) {
        return value == null
                ? ""
                : value.strip();
    }

    private static String raw(String value) {
        return value == null
                ? ""
                : value;
    }
}