package com.example.consolestore.Controller;

import com.example.consolestore.DTO.PaymentPage;
import com.example.consolestore.Service.CheckoutService;
import com.example.consolestore.Service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;
import java.util.Map;

@Controller
public class VnpayController {

    private final VnpayService vnpay;
    private final CheckoutService checkout;

    public VnpayController(
            VnpayService vnpay,
            CheckoutService checkout
    ) {
        this.vnpay = vnpay;
        this.checkout = checkout;
    }

    // Khách bấm "Thanh toán": chuyển sang trang VNPAY sandbox
    @GetMapping("/vnpay/pay/{id}")
    public ResponseEntity<Void> pay(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest request
    ) {
        if (!vnpay.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // Ném 404 nếu đơn không thuộc về khách đang đăng nhập
        PaymentPage page = checkout.payment(principal.getName(), id);

        boolean payable =
                "CHUYEN_KHOAN".equals(page.payment().getMethod())
                        && "CHUA_THANH_TOAN".equals(page.payment().getStatus())
                        && "CHO_XAC_NHAN".equals(page.invoice().getStatus());

        if (!payable) {
            return redirect("/checkout/orders/" + id + "/payment");
        }

        String url = vnpay.buildPayUrl(
                id,
                page.payment().getAmount(),
                page.invoice().getCreatedAt(),
                clientIp(request)
        );

        return redirect(url != null
                ? url
                : "/checkout/orders/" + id + "/payment?result=expired");
    }

    // VNPAY đưa trình duyệt của khách về đây sau khi thanh toán
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> back(HttpServletRequest request) {
        var result = vnpay.handle(
                VnpayService.vnpParams(request.getParameterMap())
        );

        boolean untrusted = result.invoiceId() == null
                || switch (result.outcome()) {
            case INVALID_SIGNATURE, ORDER_NOT_FOUND, AMOUNT_MISMATCH -> true;
            default -> false;
        };

        if (untrusted) {
            return redirect("/orders");
        }

        String status = switch (result.outcome()) {
            case SUCCESS, ALREADY_PAID -> "success";
            case CANCELLED -> "cancel";
            case EXPIRED -> "expired";
            default -> "failed";
        };

        return redirect("/checkout/orders/" + result.invoiceId()
                + "/payment?result=" + status);
    }

    // VNPAY gọi trực tiếp từ máy chủ (cần URL public, ví dụ ngrok)
    @GetMapping("/vnpay/ipn")
    @ResponseBody
    public Map<String, String> ipn(HttpServletRequest request) {
        var result = vnpay.handle(
                VnpayService.vnpParams(request.getParameterMap())
        );

        return switch (result.outcome()) {
            case SUCCESS, FAILED, CANCELLED ->
                    reply("00", "Confirm Success");
            case ALREADY_PAID ->
                    reply("02", "Order already confirmed");
            case INVALID_SIGNATURE ->
                    reply("97", "Invalid signature");
            case ORDER_NOT_FOUND ->
                    reply("01", "Order not found");
            case AMOUNT_MISMATCH ->
                    reply("04", "Invalid amount");
            case EXPIRED ->
                    reply("99", "Order is closed");
        };
    }

    private static Map<String, String> reply(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private static ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(location))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        // IPv6 (vd. ::1 khi chạy localhost) không được VNPAY chấp nhận
        return ip == null || ip.contains(":") ? "127.0.0.1" : ip;
    }
}
