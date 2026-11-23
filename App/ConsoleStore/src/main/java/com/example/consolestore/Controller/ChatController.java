package com.example.consolestore.Controller;

import com.example.consolestore.Config.SecurityConfig;
import com.example.consolestore.Service.ChatBridge;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/chat")
public class ChatController {

    // Vai trò được trả lời khách. Muốn thêm KHO, BAOHANH thì thêm vào đây.
    private static final Set<String> STAFF_ROLES =
            Set.of("ADMIN", "NHANVIEN", "BANHANG");

    private static final Pattern TAB_ID =
            Pattern.compile("[A-Za-z0-9_-]{8,64}");

    private static final int MAX_TEXT = 500;

    private final ChatBridge bridge;

    public ChatController(ChatBridge bridge) {
        this.bridge = bridge;
    }

    /** Cho trình duyệt biết đang là khách / nhân viên / chưa đăng nhập. */
    @GetMapping(
            value = "/config",
            produces = "text/plain;charset=UTF-8"
    )
    @ResponseBody
    public String config(CsrfToken csrf) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String role = SecurityConfig.roleOf(authentication);

        String mode;
        String name = "";

        if (STAFF_ROLES.contains(role)) {
            mode = "STAFF";
            name = authentication.getName();
        } else if ("KHACHHANG".equals(role)) {
            mode = "CUSTOMER";
            name = authentication.getName();
        } else {
            mode = "GUEST";
        }

        return String.join("\t",
                mode,
                name,
                csrf.getHeaderName(),
                csrf.getToken()
        );
    }

    /** Mở luồng nhận tin (SSE) + kết nối TCP cho tab này. */
    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(@RequestParam("tab") String tab) {
        if (!TAB_ID.matcher(tab).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String role = SecurityConfig.roleOf(authentication);

        try {
            return bridge.open(
                    tab,
                    authentication.getName(),
                    STAFF_ROLES.contains(role)
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Máy chủ chat chưa sẵn sàng."
            );
        }
    }

    /** Gửi một tin nhắn. */
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Void> send(
            @RequestParam("tab") String tab,
            @RequestParam(value = "room", defaultValue = "") String room,
            @RequestParam("text") String text
    ) {
        String clean = text.strip();

        if (!TAB_ID.matcher(tab).matches()
                || clean.isEmpty()
                || clean.length() > MAX_TEXT) {
            return ResponseEntity.badRequest().build();
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        try {
            bridge.say(tab, authentication.getName(), room, clean);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}