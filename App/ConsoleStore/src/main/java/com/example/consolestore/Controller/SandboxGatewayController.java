package com.example.consolestore.Controller;

import com.example.consolestore.Service.PaymentGatewayService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/sandbox/pay")
public class SandboxGatewayController {

    private final PaymentGatewayService gateway;
    private final int timeoutMinutes;
    private final String publicBaseUrl;

    public SandboxGatewayController(
            PaymentGatewayService gateway,
            @Value("${store.order-expiry.timeout-minutes:15}")
            int timeoutMinutes,
            @Value("${store.public-base-url:}")
            String publicBaseUrl
    ) {
        this.gateway = gateway;
        this.timeoutMinutes = timeoutMinutes;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
    }

    @GetMapping("/{id}")
    public String page(
            @PathVariable Long id,
            @RequestParam("t") String token,
            @RequestParam(value = "e", required = false) String error,
            @RequestParam(value = "m", required = false) String mobile,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        var view = gateway.view(id, token);

        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        long secondsLeft = Math.max(0, Duration.between(
                LocalDateTime.now(),
                view.createdAt().plusMinutes(timeoutMinutes)
        ).getSeconds());

        String base = publicBaseUrl.replaceAll("/+$", "");

        model.addAttribute("v", view);
        model.addAttribute("token", token);
        model.addAttribute("error", error);
        model.addAttribute("secondsLeft", secondsLeft);
        model.addAttribute("phone", "1".equals(mobile));
        model.addAttribute("qrUrl", base.isEmpty()
                ? ""
                : base + "/sandbox/pay/" + id + "?t=" + token + "&m=1");

        return "sandbox-gateway";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(
            @PathVariable Long id,
            @RequestParam("t") String token,
            @RequestParam(defaultValue = "") String otp
    ) {
        String back = "redirect:/sandbox/pay/" + id + "?t=" + token + "&m=1";

        return switch (gateway.pay(id, token, otp)) {
            case SUCCESS -> back;
            case WRONG_OTP -> back + "&e=otp";
            case NO_FUNDS -> back + "&e=funds";
            case EXPIRED -> back + "&e=expired";
            case INVALID -> throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        };
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @RequestParam("t") String token
    ) {
        return switch (gateway.cancel(id, token)) {
            case INVALID -> throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
            // Khách vừa kịp thanh toán ở điện thoại: đừng hủy, báo thành công.
            case ALREADY_PAID -> "redirect:/checkout/orders/" + id
                    + "/payment?result=success";
            case CANCELLED, CLOSED -> "redirect:/checkout/orders/" + id
                    + "/payment?result=cancel";
        };
    }
    @GetMapping("/{id}/status")
    @ResponseBody
    public Map<String, Boolean> status(
            @PathVariable Long id,
            @RequestParam("t") String token
    ) {
        var view = gateway.view(id, token);

        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return Map.of(
                "paid", view.isPaid(),
                "closed", view.isClosed()
        );
    }
}