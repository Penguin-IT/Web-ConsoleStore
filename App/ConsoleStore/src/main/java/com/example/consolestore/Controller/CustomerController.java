package com.example.consolestore.Controller;

import com.example.consolestore.DTO.CustomerForm;
import com.example.consolestore.Service.CustomerService;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
public class CustomerController {

    private static final Logger log =
            LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields(
                "fullName",
                "phone",
                "email",
                "address",
                "version"
        );
    }

    @GetMapping({
            "/customers",
            "/pages/customers.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "customers",
                service.list(
                        principal.getName(),
                        keyword,
                        page
                )
        );

        model.addAttribute("keyword", keyword);

        return "customers";
    }

    @GetMapping("/customers/new")
    public String showNew(
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return render(
                new CustomerForm(),
                null,
                model
        );
    }

    @GetMapping("/customers/{id}/edit")
    public String showEdit(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return render(
                service.form(principal.getName(), id),
                null,
                model
        );
    }

    @PostMapping("/customers")
    public String create(
            @ModelAttribute("form") CustomerForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        try {
            Long id = service.create(
                    principal.getName(),
                    form,
                    errors
            );

            if (id != null) {
                redirect.addFlashAttribute(
                        "success",
                        "Đã thêm khách hàng."
                );

                return "redirect:/customers/" + id + "/edit";
            }

            response.setStatus(400);
        } catch (DataIntegrityViolationException exception) {
            response.setStatus(409);

            errors.rejectValue(
                    "phone",
                    "customer.phone.duplicate",
                    "Số điện thoại vừa được sử dụng. Hãy kiểm tra lại."
            );
        } catch (DataAccessException exception) {
            log.warn("Cannot create customer", exception);

            response.setStatus(503);

            errors.reject(
                    "customer.unavailable",
                    "Chưa thể lưu khách hàng. Vui lòng thử lại."
            );
        }

        return render(form, errors, model);
    }

    @PostMapping("/customers/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("form") CustomerForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        form.setId(id);

        try {
            if (service.update(
                    principal.getName(),
                    id,
                    form,
                    errors
            )) {
                redirect.addFlashAttribute(
                        "success",
                        "Đã cập nhật khách hàng."
                );

                return "redirect:/customers/" + id + "/edit";
            }

            response.setStatus(400);
        } catch (DataIntegrityViolationException exception) {
            response.setStatus(409);

            errors.rejectValue(
                    "phone",
                    "customer.phone.duplicate",
                    "Số điện thoại vừa được sử dụng. Hãy kiểm tra lại."
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot update customer {}",
                    id,
                    exception
            );

            response.setStatus(503);

            errors.reject(
                    "customer.unavailable",
                    "Chưa xác nhận được kết quả. Hãy tải lại hồ sơ."
            );
        }

        return render(form, errors, model);
    }

    @PostMapping("/customers/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam String version,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            service.delete(
                    principal.getName(),
                    id,
                    version
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã xóa khách hàng."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            redirect.addFlashAttribute(
                    "error",
                    "Khách vừa phát sinh liên kết "
                            + "hoặc giao dịch, không thể xóa."
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot delete customer {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. "
                            + "Vui lòng tải lại danh sách."
            );
        }

        return "redirect:/customers";
    }

    private String render(
            CustomerForm form,
            BindingResult errors,
            Model model
    ) {
        Map<String, String> fields = new LinkedHashMap<>();
        List<String> global = new ArrayList<>();

        if (errors != null) {
            errors.getFieldErrors().forEach(error ->
                    fields.putIfAbsent(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            errors.getGlobalErrors().forEach(error ->
                    global.add(error.getDefaultMessage())
            );
        }

        model.addAttribute("form", form);
        model.addAttribute("editing", form.getId() != null);
        model.addAttribute("fieldErrors", fields);
        model.addAttribute("formErrors", global);

        return "customer-form";
    }
}