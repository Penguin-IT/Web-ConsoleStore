package com.example.consolestore.Controller;

import com.example.consolestore.DTO.ProductForm;
import com.example.consolestore.Service.ProductManagementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class ProductManagementController {

    private static final Logger log =
            LoggerFactory.getLogger(ProductManagementController.class);

    private final ProductManagementService service;

    public ProductManagementController(
            ProductManagementService service
    ) {
        this.service = service;
    }

    @GetMapping({
            "/manage-products",
            "/pages/manage-products.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "active") String status,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "products",
                service.list(
                        principal.getName(),
                        keyword,
                        status,
                        sort,
                        page
                )
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("activeStoreMenu", "products");

        return "manage-products";
    }

    @GetMapping("/manage-products/new")
    public String showNew(
            Principal principal,
            Model model
    ) {
        return render(
                principal.getName(),
                null,
                ProductForm.empty(),
                null,
                model
        );
    }

    @GetMapping("/manage-products/{id}/edit")
    public String showEdit(
            @PathVariable Long id,
            Principal principal,
            Model model
    ) {
        return render(
                principal.getName(),
                id,
                service.form(principal.getName(), id),
                null,
                model
        );
    }

    @PostMapping("/manage-products")
    public String create(
            @ModelAttribute("form") ProductForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return save(
                null,
                form,
                errors,
                principal,
                model,
                redirect
        );
    }

    @PostMapping("/manage-products/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("form") ProductForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        return save(
                id,
                form,
                errors,
                principal,
                model,
                redirect
        );
    }

    @PostMapping("/manage-products/{id}/delete")
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
                    "Đã xóa mềm sản phẩm SP" + id + "."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn("Cannot delete product {}", id, exception);

            redirect.addFlashAttribute(
                    "error",
                    "Chưa thể xóa sản phẩm. Hãy tải lại và thử lại."
            );
        }

        return "redirect:/manage-products";
    }

    @GetMapping({
            "/manage-products/categories",
            "/manage-products/platforms"
    })
    public String dictionaries(
            Principal principal,
            Model model
    ) {
        model.addAttribute(
                "categories",
                service.categoryOptions(principal.getName())
        );

        model.addAttribute(
                "platforms",
                service.platformOptions(principal.getName())
        );

        model.addAttribute("activeStoreMenu", "products");

        return "product-dictionaries";
    }

    @PostMapping("/manage-products/categories/save")
    public String saveCategory(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return dictionaryAction(
                () -> service.saveCategory(
                        principal.getName(),
                        id,
                        name
                ),
                redirect
        );
    }

    @PostMapping("/manage-products/categories/{id}/delete")
    public String deleteCategory(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return dictionaryAction(
                () -> service.deleteCategory(
                        principal.getName(),
                        id
                ),
                redirect
        );
    }

    @PostMapping("/manage-products/platforms/save")
    public String savePlatform(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return dictionaryAction(
                () -> service.savePlatform(
                        principal.getName(),
                        id,
                        name
                ),
                redirect
        );
    }

    @PostMapping("/manage-products/platforms/{id}/delete")
    public String deletePlatform(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return dictionaryAction(
                () -> service.deletePlatform(
                        principal.getName(),
                        id
                ),
                redirect
        );
    }

    private String save(
            Long id,
            ProductForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect
    ) {
        if (!errors.hasErrors()) {
            try {
                Long savedId = service.save(
                        principal.getName(),
                        id,
                        form,
                        errors
                );

                if (!errors.hasErrors() && savedId != null) {
                    redirect.addFlashAttribute(
                            "success",
                            "Đã lưu sản phẩm SP" + savedId + "."
                    );

                    return "redirect:/manage-products";
                }
            } catch (IllegalArgumentException exception) {
                errors.reject(
                        "product.invalid",
                        exception.getMessage()
                );
            } catch (DataAccessException exception) {
                log.warn("Cannot save product {}", id, exception);

                errors.reject(
                        "product.database",
                        "Chưa thể lưu. Hãy kiểm tra mã hàng cũ "
                                + "liên kết, giá bán và dữ liệu hiện tại."
                );
            }
        }

        return render(
                principal.getName(),
                id,
                form,
                errors,
                model
        );
    }

    private String render(
            String username,
            Long id,
            ProductForm form,
            BindingResult errors,
            Model model
    ) {
        model.addAttribute("productId", id);
        model.addAttribute("form", form);

        model.addAttribute(
                "categories",
                service.categoryOptions(username)
        );

        model.addAttribute(
                "platforms",
                service.platformOptions(username)
        );

        model.addAttribute(
                "baseProducts",
                service.baseProductOptions(username, id)
        );

        List<String> messages = errors == null
                ? List.of()
                : errors.getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof
                            org.springframework.validation.FieldError field
                            && field.isBindingFailure()) {
                        return "Giá hoặc mã lựa chọn không hợp lệ.";
                    }

                    return error.getDefaultMessage() == null
                            ? "Thông tin chưa hợp lệ."
                            : error.getDefaultMessage();
                })
                .distinct()
                .toList();

        model.addAttribute("formErrors", messages);
        model.addAttribute("activeStoreMenu", "products");

        return "product-form";
    }

    private String dictionaryAction(
            Runnable action,
            RedirectAttributes redirect
    ) {
        try {
            action.run();

            redirect.addFlashAttribute(
                    "success",
                    "Đã cập nhật danh mục."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn("Cannot update product dictionary", exception);

            redirect.addFlashAttribute(
                    "error",
                    "Chưa thể cập nhật. Tên có thể bị trùng "
                            + "hoặc danh mục đang được sử dụng."
            );
        }

        return "redirect:/manage-products/categories";
    }
}