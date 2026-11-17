package com.example.consolestore.Controller;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class AccountController {

    private static final Logger log =
            LoggerFactory.getLogger(AccountController.class);

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @InitBinder("profileForm")
    public void profileBinding(
            WebDataBinder binder
    ) {
        binder.setAllowedFields(
                "fullName",
                "phone",
                "address"
        );
    }

    @InitBinder("passwordForm")
    public void passwordBinding(
            WebDataBinder binder
    ) {
        binder.setAllowedFields(
                "currentPassword",
                "newPassword",
                "confirmPassword"
        );
    }

    @GetMapping({"/account","/pages/account.html"})
    public String show(
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        return render(
                principal.getName(),
                model,
                response
        );
    }

    @PostMapping("/account/profile")
    public String updateProfile(
            @ModelAttribute("profileForm")
            ProfileRequest form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store"
        );

        try {
            if (service.updateProfile(
                    principal.getName(),
                    form,
                    errors
            )) {
                redirect.addFlashAttribute(
                        "accountSuccess",
                        "Đã cập nhật thông tin cá nhân."
                );

                return "redirect:/account";
            }

            response.setStatus(400);
        } catch (DataIntegrityViolationException exception) {
            response.setStatus(409);

            errors.rejectValue(
                    "phone",
                    "profile.phone.conflict",
                    "Thông tin bị trùng. "
                            + "Vui lòng kiểm tra số điện thoại."
            );
        } catch (
                DataAccessException
                | TransactionException exception
        ) {
            response.setStatus(503);

            log.error(
                    "Cannot confirm profile update",
                    exception
            );

            errors.reject(
                    "profile.save.failed",
                    "Chưa xác nhận được kết quả lưu. "
                            + "Tải lại trang để kiểm tra."
            );
        }

        return render(
                principal.getName(),
                model,
                response
        );
    }

    @PostMapping("/account/password")
    public String changePassword(
            @ModelAttribute("passwordForm")
            PasswordChangeRequest form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store"
        );

        boolean changed = false;

        try {
            changed = service.changePassword(
                    principal.getName(),
                    form,
                    errors
            );

            if (!changed) {
                response.setStatus(400);
            }
        } catch (
                DataAccessException
                | TransactionException exception
        ) {
            response.setStatus(503);

            log.error(
                    "Cannot confirm password change",
                    exception
            );

            errors.reject(
                    "password.save.failed",
                    "Chưa xác nhận được kết quả đổi mật khẩu. "
                            + "Nếu đã đổi, hãy đăng nhập bằng mật khẩu mới."
            );
        } finally {
            form.clear();
        }

        if (changed) {
            var authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            new CookieClearingLogoutHandler(
                    "JSESSIONID"
            ).logout(
                    request,
                    response,
                    authentication
            );

            new SecurityContextLogoutHandler()
                    .logout(
                            request,
                            response,
                            authentication
                    );

            return "redirect:/login?passwordChanged";
        }

        return render(
                principal.getName(),
                model,
                response
        );
    }

    private String render(
            String username,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store"
        );

        model.addAttribute(
                "activeStoreMenu",
                "account"
        );

        model.addAttribute("account", null);

        try {
            AccountProfile profile =
                    service.profile(username);

            model.addAttribute(
                    "account",
                    profile
            );

            if (!model.containsAttribute("profileForm")) {
                model.addAttribute(
                        "profileForm",
                        new ProfileRequest(
                                profile.fullName(),
                                profile.phone(),
                                profile.address()
                        )
                );
            }
        } catch (
                DataAccessException
                | TransactionException exception
        ) {
            response.setStatus(503);

            log.error(
                    "Cannot load account profile",
                    exception
            );

            model.addAttribute(
                    "accountError",
                    "Chưa tải được thông tin tài khoản. "
                            + "Vui lòng thử lại."
            );
        }

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute(
                    "profileForm",
                    new ProfileRequest()
            );
        }

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute(
                    "passwordForm",
                    new PasswordChangeRequest()
            );
        }

        return "account";
    }
}