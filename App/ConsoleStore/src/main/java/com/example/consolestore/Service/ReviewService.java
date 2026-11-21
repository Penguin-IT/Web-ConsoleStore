package com.example.consolestore.Service;

import com.example.consolestore.Repository.DanhGiaRepository;
import com.example.consolestore.Repository.SanPhamRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    public enum State {
        GUEST,             // chưa đăng nhập
        NOT_CUSTOMER,      // nhân viên/admin
        NOT_PURCHASED,     // khách chưa mua hoặc chưa nhận hàng
        ALREADY_REVIEWED,  // đã đánh giá rồi
        CAN_REVIEW
    }

    public record ReviewView(
            String reviewer,
            int stars,
            String starText,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record Summary(double average, long total, String starText) {
    }

    private final DanhGiaRepository reviews;
    private final SanPhamRepository products;
    private final TaiKhoanRepository accounts;

    public ReviewService(
            DanhGiaRepository reviews,
            SanPhamRepository products,
            TaiKhoanRepository accounts
    ) {
        this.reviews = reviews;
        this.products = products;
        this.accounts = accounts;
    }

    public List<ReviewView> list(Long productId) {
        return reviews.latest(productId).stream()
                .map(row -> {
                    int stars = row.getStars() == null ? 0 : row.getStars();

                    return new ReviewView(
                            mask(row.getReviewer()),
                            stars,
                            starText(stars),
                            row.getContent(),
                            row.getCreatedAt()
                    );
                })
                .toList();
    }

    public Summary summary(Long productId) {
        var row = reviews.summary(productId);

        long total = row == null || row.getTotal() == null
                ? 0
                : row.getTotal();

        double average = row == null || row.getAverage() == null
                ? 0
                : row.getAverage();

        return new Summary(
                average,
                total,
                starText((int) Math.round(average))
        );
    }

    public State stateFor(
            String username,
            boolean customerRole,
            Long productId
    ) {
        if (username == null) {
            return State.GUEST;
        }

        if (!customerRole) {
            return State.NOT_CUSTOMER;
        }

        Optional<Long> customerId = findCustomerId(username);

        if (customerId.isEmpty()) {
            return State.NOT_CUSTOMER;
        }

        if (reviews.countByProductAndCustomer(
                productId,
                customerId.get()
        ) > 0) {
            return State.ALREADY_REVIEWED;
        }

        return reviews.countCompletedPurchases(
                customerId.get(),
                productId
        ) > 0
                ? State.CAN_REVIEW
                : State.NOT_PURCHASED;
    }

    @Transactional(timeout = 15)
    public void submit(
            String username,
            Long productId,
            Integer stars,
            String content
    ) {
        Long customerId = findCustomerId(username)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Customer access required"
                        )
                );

        if (stars == null || stars < 1 || stars > 5) {
            throw bad("Số sao phải từ 1 đến 5.");
        }

        String text = content == null ? "" : content.strip();

        if (text.length() < 10 || text.length() > 1000) {
            throw bad("Nội dung đánh giá từ 10 đến 1000 ký tự.");
        }

        if (products.findByMaSPAndDeletedFalse(productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (reviews.countCompletedPurchases(customerId, productId) == 0) {
            throw bad(
                    "Chỉ khách đã mua và nhận sản phẩm này "
                            + "mới được đánh giá."
            );
        }

        if (reviews.countByProductAndCustomer(
                productId,
                customerId
        ) > 0) {
            throw bad("Bạn đã đánh giá sản phẩm này rồi.");
        }

        reviews.insert(productId, customerId, stars, text);
    }

    private Optional<Long> findCustomerId(String username) {
        return accounts.findWithCustomer(username)
                .filter(account ->
                        !account.isLocked()
                                && "KHACHHANG".equals(account.getRole())
                                && account.getCustomer() != null
                )
                .map(account -> account.getCustomer().getId());
    }

    /** "Nguyễn Văn An" -> "Nguyễn *** An" */
    private static String mask(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Khách hàng";
        }

        String[] parts = fullName.strip().split("\\s+");

        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }

        return parts[0] + " *** " + parts[parts.length - 1];
    }

    private static String starText(int stars) {
        int value = Math.max(0, Math.min(5, stars));

        return "★".repeat(value) + "☆".repeat(5 - value);
    }

    private static IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}