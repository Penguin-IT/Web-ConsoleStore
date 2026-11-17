package com.example.consolestore.Controller;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Service.StaffService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class StaffController {

    private static final Logger log =
            LoggerFactory.getLogger(StaffController.class);

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping({"/staff", "/pages/staff.html"})
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "employees",
                service.list(principal.getName(), keyword, page)
        );

        model.addAttribute("keyword", keyword);

        return "staff";
    }

    @GetMapping("/staff/new")
    public String showNew(
            Principal principal,
            Model model
    ) {
        service.list(principal.getName(), "", 0);

        return renderEmployee(
                null,
                StaffForm.empty(),
                model
        );
    }

    @GetMapping("/staff/{id}/edit")
    public String showEdit(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        return renderEmployee(
                id,
                service.form(principal.getName(), id),
                model
        );
    }

    @PostMapping("/staff")
    public String create(
            @ModelAttribute("form") StaffForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return saveEmployee(
                null, form, errors,
                principal, model, redirect
        );
    }

    @PostMapping("/staff/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("form") StaffForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return saveEmployee(
                id, form, errors,
                principal, model, redirect
        );
    }

    @GetMapping("/staff/accounts")
    public String accounts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "accounts",
                service.accountList(
                        principal.getName(),
                        keyword,
                        page
                )
        );

        model.addAttribute("keyword", keyword);

        return "staff-accounts";
    }

    @GetMapping("/staff/{id}/account/new")
    public String showNewAccount(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        var employee = service.employee(principal.getName(), id);

        model.addAttribute(
                "owner",
                employee.getFullName()
        );

        return renderAccount(
                null,
                StaffAccountForm.empty(id),
                model
        );
    }

    @GetMapping("/staff/accounts/{id}/edit")
    public String showAccount(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        return renderAccount(
                id,
                service.accountForm(principal.getName(), id),
                model
        );
    }

    @PostMapping("/staff/accounts")
    public String createAccount(
            @ModelAttribute("form") StaffAccountForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return saveAccount(
                null, form, errors,
                principal, model, redirect
        );
    }

    @PostMapping("/staff/accounts/{id}")
    public String updateAccount(
            @PathVariable Long id,
            @ModelAttribute("form") StaffAccountForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return saveAccount(
                id, form, errors,
                principal, model, redirect
        );
    }

    private String saveEmployee(
            Long id,
            StaffForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Thông tin hoặc trạng thái không hợp lệ."
            );

            return renderEmployee(id, form, model);
        }

        try {
            service.save(principal.getName(), id, form);

            redirect.addFlashAttribute(
                    "success",
                    "Đã lưu hồ sơ nhân viên."
            );

            return "redirect:/staff";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (DataAccessException exception) {
            log.warn("Cannot save employee {}", id, exception);

            model.addAttribute(
                    "error",
                    "Chưa thể lưu hồ sơ. "
                            + "Hãy kiểm tra số điện thoại hoặc tải lại trang."
            );
        }

        return renderEmployee(id, form, model);
    }

    private String saveAccount(
            Long id,
            StaffAccountForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Thông tin tài khoản không hợp lệ."
            );

            return renderAccount(id, form, model);
        }

        try {
            if (id == null) {
                service.createAccount(principal.getName(), form);
            } else {
                service.updateAccount(principal.getName(), id, form);
            }

            redirect.addFlashAttribute(
                    "success",
                    "Đã cập nhật tài khoản."
            );

            return "redirect:/staff/accounts";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (DataAccessException exception) {
            log.warn("Cannot save staff account {}", id, exception);

            model.addAttribute(
                    "error",
                    "Chưa thể lưu tài khoản. "
                            + "Tên đăng nhập có thể đã tồn tại "
                            + "hoặc dữ liệu vừa thay đổi."
            );
        }

        return renderAccount(id, form, model);
    }

    private String renderEmployee(
            Long id,
            StaffForm form,
            Model model
    ) {
        model.addAttribute("employeeId", id);
        model.addAttribute("form", form);

        return "staff-form";
    }

    private String renderAccount(
            Long id,
            StaffAccountForm form,
            Model model
    ) {
        model.addAttribute("accountId", id);
        model.addAttribute("form", form);

        return "staff-account-form";
    }
}