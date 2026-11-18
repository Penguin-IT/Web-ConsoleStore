package com.example.consolestore.Config;

import com.example.consolestore.Repository.TaiKhoanRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountStatusFilter extends OncePerRequestFilter {

    private final TaiKhoanRepository accounts;

    public AccountStatusFilter(TaiKhoanRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof
                AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }

        TaiKhoanRepository.LoginState state;

        try {
            state = accounts.loginState(
                    authentication.getName()
            ).orElse(null);
        } catch (DataAccessException exception) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Tạm thời không thể kiểm tra tài khoản."
            );

            return;
        }

        boolean valid = state != null && state.isUsable();

        if (valid) {
            String expected = "ROLE_" + state.getRole();

            valid = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            expected.equals(authority.getAuthority())
                    );
        }

        if (!valid) {
            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            String accept = request.getHeader("Accept");

            if (accept != null
                    && accept.contains("application/json")) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Vui lòng đăng nhập lại."
                );
            } else {
                response.sendRedirect(
                        request.getContextPath()
                                + "/login?expired"
                );
            }

            return;
        }

        chain.doFilter(request, response);
    }
}