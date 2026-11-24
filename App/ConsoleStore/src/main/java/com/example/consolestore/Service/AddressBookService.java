package com.example.consolestore.Service;

import com.example.consolestore.DTO.AddressRequest;
import com.example.consolestore.DTO.AddressView;
import com.example.consolestore.DTO.CheckoutRequest;
import com.example.consolestore.Entity.DiaChiGiaoHang;
import com.example.consolestore.Entity.TaiKhoan;
import com.example.consolestore.Repository.DiaChiGiaoHangRepository;
import com.example.consolestore.Repository.KhachHangRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AddressBookService {

    public static final int MAX_ADDRESSES = 10;

    private final DiaChiGiaoHangRepository addresses;
    private final KhachHangRepository customers;
    private final TaiKhoanRepository accounts;

    public AddressBookService(DiaChiGiaoHangRepository addresses,
                              KhachHangRepository customers,
                              TaiKhoanRepository accounts) {
        this.addresses = addresses;
        this.customers = customers;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<AddressView> list(String username) {
        return addresses
                .findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(customerId(username))
                .stream().map(AddressView::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<AddressView> defaultOf(String username) {
        return list(username).stream().filter(AddressView::defaultAddress).findFirst();
    }

    /** id == null: thêm mới; id != null: cập nhật. */
    @Transactional
    public AddressView save(String username, Long id, AddressRequest req) {
        Long customerId = customerId(username);

        CheckoutRequest check = new CheckoutRequest();
        check.setFullName(req.getFullName());
        check.setPhone(req.getPhone());
        check.setProvince(req.getProvince());
        check.setDistrict(req.getDistrict());
        check.setWard(req.getWard());
        check.setStreet(req.getStreet());

        Map<String, String> errors = CheckoutValidator.validate(check);
        if (!errors.isEmpty()) {
            throw new AddressRejectedException(errors);
        }

        long count = addresses.countByCustomerId(customerId);
        if (id == null && count >= MAX_ADDRESSES) {
            throw new AddressRejectedException(
                    Map.of("address", "Tối đa " + MAX_ADDRESSES + " địa chỉ."));
        }

        // Địa chỉ đầu tiên luôn là mặc định.
        boolean makeDefault = req.isMakeDefault() || count == 0;
        if (makeDefault) {
            addresses.clearDefault(customerId);   // trước khi load entity
        }

        DiaChiGiaoHang entity = id == null
                ? new DiaChiGiaoHang(customerId)
                : find(id, customerId);

        entity.fill(check.getFullName(), check.getPhone(), check.getProvince(),
                check.getDistrict(), check.getWard(), check.getStreet(), check.getAddress());

        if (makeDefault) {
            entity.setDefaultAddress(true);
        }

        entity = addresses.save(entity);

        if (entity.isDefaultAddress()) {
            syncProfileAddress(customerId, entity.getFullAddress());
        }

        return AddressView.from(entity);
    }

    /** Gọi sau khi đặt hàng thành công, form đã được CheckoutValidator chuẩn hóa. */
    @Transactional
    public void saveFromCheckout(String username, CheckoutRequest form) {
        Long customerId = customerId(username);

        if (addresses.existsByCustomerIdAndPhoneAndFullAddress(
                customerId, form.getPhone(), form.getAddress())) {
            return;   // đã có địa chỉ này rồi
        }

        AddressRequest req = new AddressRequest();
        req.setFullName(form.getFullName());
        req.setPhone(form.getPhone());
        req.setProvince(form.getProvince());
        req.setDistrict(form.getDistrict());
        req.setWard(form.getWard());
        req.setStreet(form.getStreet());
        req.setMakeDefault(form.isMakeDefaultAddress());

        save(username, null, req);
    }

    @Transactional
    public void makeDefault(String username, Long id) {
        Long customerId = customerId(username);
        find(id, customerId);                       // kiểm tra quyền sở hữu
        addresses.clearDefault(customerId);
        DiaChiGiaoHang a = find(id, customerId);    // load lại sau khi clear
        a.setDefaultAddress(true);
        syncProfileAddress(customerId, a.getFullAddress());
    }

    @Transactional
    public void delete(String username, Long id) {
        Long customerId = customerId(username);
        DiaChiGiaoHang a = find(id, customerId);
        boolean wasDefault = a.isDefaultAddress();

        addresses.delete(a);
        addresses.flush();

        // Xóa địa chỉ mặc định thì chuyển mặc định sang địa chỉ mới nhất còn lại.
        if (wasDefault) {
            addresses.findFirstByCustomerIdOrderByCreatedAtDesc(customerId)
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        syncProfileAddress(customerId, next.getFullAddress());
                    });
        }
    }

    // Giữ KhachHang.diaChi khớp với địa chỉ mặc định để trang tài khoản
    // và danh sách khách hàng của nhân viên không bị lệch dữ liệu.
    private void syncProfileAddress(Long customerId, String fullAddress) {
        customers.findById(customerId).ifPresent(c -> c.setAddress(fullAddress));
    }

    private DiaChiGiaoHang find(Long id, Long customerId) {
        return addresses.findByIdAndCustomerId(id, customerId).orElseThrow(
                () -> new AddressRejectedException(
                        Map.of("address", "Không tìm thấy địa chỉ.")));
    }

    private Long customerId(String username) {
        TaiKhoan account = accounts.findWithCustomer(username).orElseThrow(
                () -> new AccessDeniedException("Account is missing"));

        if (account.isLocked()
                || !"KHACHHANG".equals(account.getRole())
                || account.getCustomer() == null) {
            throw new AccessDeniedException("Customer access required");
        }
        return account.getCustomer().getId();
    }
}