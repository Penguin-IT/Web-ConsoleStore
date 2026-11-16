package com.example.consolestore.Service;

import com.example.consolestore.Dto.SupplierForm;
import com.example.consolestore.Entity.NhaCungCap;
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
public class SupplierService {

    private final NhaCungCapRepository suppliers;
    private final TaiKhoanRepository accounts;

    public SupplierService(NhaCungCapRepository suppliers, TaiKhoanRepository accounts) {
        this.suppliers = suppliers;
        this.accounts = accounts;
    }

    public Page<SupplierForm> list(
            String username,
            String keyword,
            int page
    ) {
        admin(username);

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

        return suppliers.search(
                query,
                PageRequest.of(Math.max(0, page), 20)
        ).map(SupplierForm::from);
    }

    public SupplierForm form(
            String username,
            Long id
    ) {
        admin(username);

        return SupplierForm.from(
                suppliers.findById(id)
                        .orElseThrow(SupplierService::notFound)
        );
    }

    @Transactional(timeout = 20)
    public Long create(
            String username,
            SupplierForm form,
            BindingResult errors
    ) {
        admin(username);

        if (!validate(form, errors)) {
            return null;
        }

        NhaCungCap supplier = new NhaCungCap(
                form.getName(),
                optional(form.getPhone()),
                optional(form.getEmail()),
                optional(form.getAddress())
        );

        return suppliers.saveAndFlush(supplier).getId();
    }

    @Transactional(timeout = 20)
    public boolean update(
            String username,
            Long id,
            SupplierForm form,
            BindingResult errors
    ) {
        admin(username);

        if (!validate(form, errors)) {
            return false;
        }

        NhaCungCap supplier = suppliers.lockById(id)
                .orElseThrow(SupplierService::notFound);

        if (!Objects.equals(
                form.getVersion(),
                SupplierForm.versionOf(supplier)
        )) {
            errors.reject(
                    "supplier.changed",
                    "Thông tin đã được cập nhật ở nơi khác. "
                            + "Hãy tải lại trước khi sửa tiếp."
            );

            return false;
        }

        supplier.updateDetails(
                form.getName(),
                optional(form.getPhone()),
                optional(form.getEmail()),
                optional(form.getAddress())
        );

        suppliers.saveAndFlush(supplier);

        return true;
    }

    @Transactional(timeout = 20)
    public void delete(
            String username,
            Long id,
            String version
    ) {
        admin(username);

        var found = suppliers.lockById(id);

        if (found.isEmpty()) {
            return;
        }

        if (!Objects.equals(
                version,
                SupplierForm.versionOf(found.get())
        )) {
            throw new IllegalArgumentException(
                    "Thông tin vừa thay đổi. "
                            + "Vui lòng tải lại danh sách."
            );
        }

        if (suppliers.purchaseCount(id) > 0) {
            throw new IllegalArgumentException(
                    "Không thể xóa nhà cung cấp đã có phiếu nhập."
            );
        }

        if (suppliers.deleteIfUnused(id) != 1) {
            throw new IllegalArgumentException(
                    "Nhà cung cấp vừa phát sinh phiếu nhập, "
                            + "không thể xóa."
            );
        }
    }

    private boolean validate(
            SupplierForm form,
            BindingResult errors
    ) {
        form.setName(clean(form.getName()));
        form.setPhone(clean(form.getPhone()));
        form.setEmail(clean(form.getEmail()));
        form.setAddress(clean(form.getAddress()));

        if (form.getName().isEmpty()
                || form.getName().length() > 100) {
            errors.rejectValue(
                    "name",
                    "supplier.name",
                    "Tên nhà cung cấp bắt buộc, tối đa 100 ký tự."
            );
        }

        if (!form.getPhone().isEmpty()
                && !form.getPhone().matches("[0-9]{1,10}")) {
            errors.rejectValue(
                    "phone",
                    "supplier.phone",
                    "Điện thoại chỉ gồm chữ số, tối đa 10 ký tự."
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
                    "supplier.email",
                    "Email không hợp lệ hoặc vượt 100 ký tự."
            );
        }

        if (form.getAddress().length() > 255) {
            errors.rejectValue(
                    "address",
                    "supplier.address",
                    "Địa chỉ tối đa 255 ký tự."
            );
        }

        return !errors.hasErrors();
    }

    private void admin(String username) {
        if (accounts.paymentAdmin(username).isEmpty()) {
            throw new AccessDeniedException(
                    "Admin access required"
            );
        }
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
                "Không tìm thấy nhà cung cấp."
        );
    }
}