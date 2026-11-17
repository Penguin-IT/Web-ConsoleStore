package com.example.consolestore.Controller;

import com.example.consolestore.DTO.RegisterRequest;
import com.example.consolestore.Service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.consolestore.Config.SecurityConfig;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Controller
public class AuthController {

    private static final String REGISTRATION_TOKEN =
            "registrationToken";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/register", "/pages/register.html"})
    public String showRegister(
            Model model,
            HttpSession session
    ) {
        model.addAttribute("form", new RegisterRequest());
        preparePage(model, session);

        return "register";
    }

    @PostMapping({"/register", "/pages/register.html"})
    public String register(
            @ModelAttribute("form") RegisterRequest form,
            BindingResult errors,
            @RequestParam(
                    name = "_registrationToken",
                    required = false
            ) String submittedToken,
            HttpSession session,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!isValidToken(session, submittedToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            errors.reject(
                    "register.token.invalid",
                    "Phiên đăng ký không hợp lệ. Vui lòng tải lại trang."
            );

            return showErrors(form, model, session);
        }

        try {
            if (!authService.register(form, errors)) {
                return showErrors(form, model, session);
            }
        } catch (DataIntegrityViolationException exception) {
            // Transaction đã rollback trước khi xử lý lỗi tại đây.
            errors.reject(
                    "register.duplicate",
                    "Tên đăng nhập hoặc số điện thoại vừa được sử dụng. "
                            + "Vui lòng kiểm tra lại."
            );

            return showErrors(form, model, session);
        } catch (DataAccessException exception) {
            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );

            errors.reject(
                    "register.unavailable",
                    "Hiện chưa thể đăng ký tài khoản. "
                            + "Vui lòng thử lại sau."
            );

            return showErrors(form, model, session);
        }

        session.removeAttribute(REGISTRATION_TOKEN);

        redirectAttributes.addFlashAttribute(
                "registrationSuccess",
                true
        );

        return "redirect:/register";
    }

    private String showErrors(
            RegisterRequest form,
            Model model,
            HttpSession session
    ) {
        form.setPassword("");
        form.setConfirmPassword("");

        preparePage(model, session);

        return "register";
    }

    private void preparePage(
            Model model,
            HttpSession session
    ) {
        Object token = session.getAttribute(REGISTRATION_TOKEN);

        if (token == null) {
            token = UUID.randomUUID().toString();

            session.setAttribute(
                    REGISTRATION_TOKEN,
                    token
            );
        }

        model.addAttribute("registrationToken", token);
        model.addAttribute("activeStoreMenu", "register");
    }

    private boolean isValidToken(
            HttpSession session,
            String submittedToken
    ) {
        Object expectedToken = session.getAttribute(
                REGISTRATION_TOKEN
        );

        if (!(expectedToken instanceof String expected)
                || submittedToken == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submittedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
    @GetMapping({"/login", "/pages/login.html"})
    public String showLogin(Model model) {
        model.addAttribute("activeStoreMenu", "login");

        return "login";
    }

    @GetMapping("/auth/session")
    @ResponseBody
    public ResponseEntity<SessionState> getSessionState(
            Authentication authentication,
            CsrfToken csrfToken
    ) {
        String role = SecurityConfig.roleOf(authentication);
        boolean authenticated = !"GUEST".equals(role);

        String username = authenticated
                ? authentication.getName()
                : "";

        SessionState state = new SessionState(
                authenticated,
                username,
                role,
                SecurityConfig.landingPage(role),
                SecurityConfig.allowedPages(role),
                SecurityConfig.restrictedPages(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(state);
    }

    public record SessionState(
            boolean authenticated,
            String username,
            String role,
            String landingPage,
            List<String> allowedPages,
            List<String> restrictedPages,
            String csrfParameter,
            String csrfToken
    ) {
    }
}