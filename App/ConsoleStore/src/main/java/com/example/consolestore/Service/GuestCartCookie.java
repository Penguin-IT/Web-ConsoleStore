package com.example.consolestore.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

public final class GuestCartCookie {

    public static final String NAME = "CART_ID";

    private static final Duration MAX_AGE = Duration.ofDays(30);
    private static final Pattern UUID_FORMAT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
                    + "-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private GuestCartCookie() {
    }

    /** Trả về mã giỏ vãng lai, hoặc null nếu không có / sai định dạng. */
    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && UUID_FORMAT.matcher(cookie.getValue()).matches()) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public static String issue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String token = UUID.randomUUID().toString();
        write(request, response, token, MAX_AGE);
        return token;
    }

    public static void clear(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        write(request, response, "", Duration.ZERO);
    }

    private static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            String value,
            Duration maxAge
    ) {
        String path = request.getContextPath().isEmpty()
                ? "/"
                : request.getContextPath();

        ResponseCookie cookie = ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}