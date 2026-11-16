package com.example.consolestore.Service;

import com.example.consolestore.DTO.CustomerForm;
import com.example.consolestore.Entity.KhachHang;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final KhachHangRepository customers;
    private final TaiKhoanRepository accounts;

    public CustomerService(KhachHangRepository customers, TaiKhoanRepository accounts) {
        this.customers = customers;
        this.accounts = accounts;
    }

    public Page<CustomerForm> list(
            String username,
            String keyword,
            int page
    ) {
        staff(username);

        String query = clean(keyword);

        if (query.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Từ khóa tối đa 100 ký tự."
            );
        }

        query = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return customers.searchCustomers(
                query,
                PageRequest.of(Math.max(0, page), 20)
        ).map(CustomerForm::from);
    }

    public CustomerForm form(
            String username,
            Long id
    ) {
        staff(username);

        return CustomerForm.from(
                customers.findById(id)
                        .orElseThrow(CustomerService::notFound)
        );
    }

    @Transactional(timeout = 20)
    public Long create(
            String username,
            CustomerForm form,
            BindingResult errors
    ) {
        staff(username);

        if (!validate(form, errors)) {
            return null;
        }

        if (customers.existsByPhone(form.getPhone())) {
            duplicatePhone(errors);
            return null;
        }

        KhachHang customer = new KhachHang(
                form.getFullName(),
                form.getPhone(),
                optional(form.getEmail())
        );

        customer.updateDetails(
                form.getFullName(),
                form.getPhone(),
                optional(form.getEmail()),
                optional(form.getAddress())
        );

        return customers.saveAndFlush(customer).getId();
    }

    @Transactional(timeout = 20)
    public boolean update(
            String username,
            Long id,
            CustomerForm form,
            BindingResult errors
    ) {
        staff(username);

        if (!validate(form, errors)) {
            return false;
        }

        KhachHang customer = customers.lockById(id)
                .orElseThrow(CustomerService::notFound);

        if (!Objects.equals(
                form.getVersion(),
                CustomerForm.versionOf(customer)
        )) {
            errors.reject(
                    "customer.changed",
                    "Hồ sơ đã được cập nhật ở nơi khác. "
                            + "Hãy tải lại trước khi sửa tiếp."
            );

            return false;
        }

        if (customers.existsByPhoneAndIdNot(
                form.getPhone(),
                id
        )) {
            duplicatePhone(errors);
            return false;
        }

        customer.updateDetails(
                form.getFullName(),
                form.getPhone(),
                optional(form.getEmail()),
                optional(form.getAddress())
        );

        customers.saveAndFlush(customer);

        return true;
    }

    @Transactional(timeout = 20)
    public void delete(
            String username,
            Long id,
            String version
    ) {
        staff(username);

        var found = customers.lockById(id);

        if (found.isEmpty()) {
            return;
        }

        if (!Objects.equals(
                version,
                CustomerForm.versionOf(found.get())
        )) {
            throw new IllegalArgumentException(
                    "Hồ sơ vừa thay đổi. "
                            + "Vui lòng tải lại danh sách."
            );
        }

        var links = customers.customerLinks(id);

        if (links.getInvoiceCount() > 0
                || links.getTradeInCount() > 0) {
            throw new IllegalArgumentException(
                    "Không thể xóa khách đã có "
                            + "hóa đơn hoặc phiếu thu cũ."
            );
        }

        if (links.getAccountCount() > 0) {
            throw new IllegalArgumentException(
                    "Khách đang gắn tài khoản đăng nhập, "
                            + "không thể xóa hồ sơ."
            );
        }

        if (customers.deleteIfUnused(id) != 1) {
            throw new IllegalArgumentException(
                    "Khách vừa phát sinh liên kết "
                            + "hoặc giao dịch, không thể xóa."
            );
        }
    }

    private boolean validate(
            CustomerForm form,
            BindingResult errors
    ) {
        form.setFullName(clean(form.getFullName()));
        form.setPhone(clean(form.getPhone()));
        form.setEmail(clean(form.getEmail()));
        form.setAddress(clean(form.getAddress()));

        if (form.getFullName().isEmpty()
                || form.getFullName().length() > 100) {
            errors.rejectValue(
                    "fullName",
                    "customer.name",
                    "Họ tên bắt buộc, tối đa 100 ký tự."
            );
        }

        if (!form.getPhone().matches("0[0-9]{9}")) {
            errors.rejectValue(
                    "phone",
                    "customer.phone",
                    "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0."
            );
        }

        if (form.getEmail().length() > 100
                || (
                !form.getEmail().isEmpty()
                        && !form.getEmail().matches(
                        "[^\\s@]+@[^\\s@]+\\.[^\\s@]+"
                )
        )) {
            errors.rejectValue(
                    "email",
                    "customer.email",
                    "Email không hợp lệ hoặc vượt 100 ký tự."
            );
        }

        if (form.getAddress().length() > 255) {
            errors.rejectValue(
                    "address",
                    "customer.address",
                    "Địa chỉ tối đa 255 ký tự."
            );
        }

        return !errors.hasErrors();
    }

    private void staff(String username) {
        if (accounts.posStaff(username).isEmpty()) {
            throw new AccessDeniedException(
                    "Staff access required"
            );
        }
    }

    private static void duplicatePhone(BindingResult errors) {
        errors.rejectValue(
                "phone",
                "customer.phone.duplicate",
                "Số điện thoại đã được khách hàng khác sử dụng."
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static String optional(String value) {
        return value.isEmpty() ? null : value;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy khách hàng."
        );
    }
}