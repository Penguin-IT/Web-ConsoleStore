package com.example.consolestore.Controller;

import com.example.consolestore.Config.SecurityConfig;
import com.example.consolestore.Service.AiRateLimiter;
import com.example.consolestore.Service.AiShopService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API cho khung chat AI: POST /ai/chat  {"message":"tôi muốn dưới 2 triệu"} */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private static final int MAX_LENGTH = 200;

    public record ChatRequest(String message) {}

    private final AiShopService shop;
    private final AiRateLimiter limiter;
    private final boolean enabled;

    public AiChatController(AiShopService shop, AiRateLimiter limiter,
                            @Value("${store.ai.enabled:true}") boolean enabled) {
        this.shop = shop;
        this.limiter = limiter;
        this.enabled = enabled;
    }

    /** Trình duyệt hỏi: có hiện khung chat AI không? Chỉ hiện cho khách vãng lai và khách hàng. */
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> config() {
        String role = SecurityConfig.roleOf(SecurityContextHolder.getContext().getAuthentication());
        return Map.of("enabled", enabled && (role.equals("GUEST") || role.equals("KHACHHANG")));
    }

    @PostMapping(value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody ChatRequest request, HttpServletRequest http) {
        if (!enabled) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Trợ lý AI đang tạm tắt.");
        }

        String message = request.message() == null ? "" : request.message().strip();

        if (message.isEmpty() || message.length() > MAX_LENGTH) {
            return error(HttpStatus.BAD_REQUEST, "Tin nhắn cần từ 1 đến " + MAX_LENGTH + " ký tự.");
        }
        if (!limiter.allow(clientIp(http))) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Bạn gửi hơi nhanh, đợi một chút rồi thử lại nhé.");
        }

        try {
            return ResponseEntity.ok(shop.chat(message));
        } catch (DataAccessException e) {
            log.error("AI chat khong doc duoc san pham: {}", e.getClass().getSimpleName());
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Chưa tải được sản phẩm, vui lòng thử lại sau.");
        }
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    // Chạy sau ngrok/proxy thì IP thật nằm trong X-Forwarded-For.
    private static String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].strip();
        return http.getRemoteAddr();
    }
}
