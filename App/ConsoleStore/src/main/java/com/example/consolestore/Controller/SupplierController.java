package com.example.consolestore.Controller;

import com.example.consolestore.DTO.SupplierForm;
import com.example.consolestore.Service.SupplierService;

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
public class SupplierController {

    private static final Logger log =
            LoggerFactory.getLogger(SupplierController.class);

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields(
                "name",
                "phone",
                "email",
                "address",
                "version"
        );
    }

    @GetMapping({
            "/suppliers",
            "/pages/suppliers.html"
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
                "suppliers",
                service.list(
                        principal.getName(),
                        keyword,
                        page
                )
        );

        model.addAttribute("keyword", keyword);

        return "suppliers";
    }

    @GetMapping("/suppliers/new")
    public String showNew(
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return render(
                new SupplierForm(),
                null,
                model
        );
    }

    @GetMapping("/suppliers/{id}/edit")
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

    @PostMapping("/suppliers")
    public String create(
            @ModelAttribute("form") SupplierForm form,
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
                        "Đã thêm nhà cung cấp."
                );

                return "redirect:/suppliers/" + id + "/edit";
            }

            response.setStatus(400);
        } catch (DataIntegrityViolationException exception) {
            response.setStatus(400);

            errors.reject(
                    "supplier.invalid",
                    "Không thể lưu nhà cung cấp với thông tin này."
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot create supplier",
                    exception
            );

            response.setStatus(503);

            errors.reject(
                    "supplier.unavailable",
                    "Chưa xác nhận được kết quả. "
                            + "Hãy kiểm tra danh sách trước khi thêm lại."
            );
        }

        return render(form, errors, model);
    }

    @PostMapping("/suppliers/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("form") SupplierForm form,
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
                        "Đã cập nhật nhà cung cấp."
                );

                return "redirect:/suppliers/" + id + "/edit";
            }

            response.setStatus(400);
        } catch (DataIntegrityViolationException exception) {
            response.setStatus(400);

            errors.reject(
                    "supplier.invalid",
                    "Thông tin nhà cung cấp không hợp lệ."
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot update supplier {}",
                    id,
                    exception
            );

            response.setStatus(503);

            errors.reject(
                    "supplier.unavailable",
                    "Chưa xác nhận được kết quả. "
                            + "Hãy tải lại thông tin nhà cung cấp."
            );
        }

        return render(form, errors, model);
    }

    @PostMapping("/suppliers/{id}/delete")
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
                    "Đã xóa nhà cung cấp."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            redirect.addFlashAttribute(
                    "error",
                    "Nhà cung cấp đang được sử dụng "
                            + "trong chứng từ, không thể xóa."
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot delete supplier {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. "
                            + "Vui lòng tải lại danh sách."
            );
        }

        return "redirect:/suppliers";
    }

    private String render(
            SupplierForm form,
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

        return "supplier-form";
    }
}