package com.example.consolestore.Service;

import com.example.consolestore.Entity.GioHang;
import com.example.consolestore.Entity.TaiKhoan;
import com.example.consolestore.Repository.GioHangRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CartStore {

    // Trần mỗi dòng khi gộp giỏ (tránh tràn số). Trang giỏ vẫn cảnh báo nếu vượt tồn kho.
    private static final int MAX_LINE_QUANTITY = 99;

    /** Chủ giỏ: khách hàng (customerId) hoặc khách vãng lai (guestToken). */
    public record Owner(Long customerId, String guestToken) {

        public static Owner customer(Long id) {
            return new Owner(id, null);
        }

        public static Owner guest(String token) {
            return new Owner(null, token);
        }

        public boolean isGuest() {
            return customerId == null;
        }

        public String key() {
            return customerId != null
                    ? "c:" + customerId
                    : "g:" + guestToken;
        }
    }

    private final GioHangRepository carts;
    private final TaiKhoanRepository accounts;

    public CartStore(
            GioHangRepository carts,
            TaiKhoanRepository accounts
    ) {
        this.carts = carts;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Owner customerOwner(String username) {
        TaiKhoan account = accounts.findWithCustomer(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "Account is missing"
                ));

        if (account.isLocked()
                || !"KHACHHANG".equals(account.getRole())
                || account.getCustomer() == null) {
            throw new AccessDeniedException("Customer access required");
        }

        return Owner.customer(account.getCustomer().getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> load(Owner owner) {
        Optional<GioHang> cart = find(owner);

        if (cart.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> items = new LinkedHashMap<>();

        for (GioHangRepository.Line line : carts.lines(cart.get().getId())) {
            items.put(line.getProductId(), line.getQuantity());
        }

        return items;
    }

    @Transactional
    public void save(Owner owner, Map<Long, Integer> items) {
        Long cartId = lockOrCreate(owner);

        carts.clearLines(cartId);

        int position = 0;

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            carts.addLine(
                    cartId,
                    entry.getKey(),
                    entry.getValue(),
                    position++
            );
        }

        carts.touch(cartId);
    }

    /** Gộp giỏ vãng lai vào giỏ khách hàng. Trả về true nếu có sản phẩm được gộp. */
    @Transactional
    public boolean mergeGuestCart(String guestToken, Long customerId) {
        Optional<GioHang> guest = carts.findByGuestToken(guestToken);

        if (guest.isEmpty()) {
            return false;
        }

        Owner customer = Owner.customer(customerId);
        Map<Long, Integer> merged = new LinkedHashMap<>(load(customer));
        boolean any = false;

        for (GioHangRepository.Line line : carts.lines(guest.get().getId())) {
            merged.merge(
                    line.getProductId(),
                    line.getQuantity(),
                    (a, b) -> Math.min(MAX_LINE_QUANTITY, a + b)
            );
            any = true;
        }

        if (any) {
            save(customer, merged);
        }

        carts.deleteCart(guest.get().getId());

        return any;
    }

    @Transactional
    public int purgeStaleGuestCarts(int days) {
        return carts.deleteStaleGuestCarts(
                LocalDateTime.now().minusDays(days)
        );
    }

    private Optional<GioHang> find(Owner owner) {
        if (owner.customerId() != null) {
            return carts.findByCustomerId(owner.customerId());
        }

        if (owner.guestToken() != null) {
            return carts.findByGuestToken(owner.guestToken());
        }

        return Optional.empty();
    }

    // Tạo giỏ nếu chưa có, rồi khóa dòng giỏ để 2 lần ghi không đè nhau
    private Long lockOrCreate(Owner owner) {
        if (owner.customerId() != null) {
            carts.createForCustomer(owner.customerId());
        } else if (owner.guestToken() != null) {
            carts.createForGuest(owner.guestToken());
        } else {
            throw new IllegalStateException("Giỏ hàng chưa có chủ sở hữu");
        }

        Long id = find(owner).orElseThrow().getId();

        return carts.lockCart(id).orElseThrow().getId();
    }
}