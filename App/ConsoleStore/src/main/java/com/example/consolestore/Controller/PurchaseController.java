package com.example.consolestore.Controller;

import com.example.consolestore.DTO.PurchaseRequest;
import com.example.consolestore.Service.PurchaseService;

import jakarta.servlet.http.*;

import org.slf4j.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
public class PurchaseController {

    private static final String TOKEN = "purchaseToken";

    private static final Logger log =
            LoggerFactory.getLogger(PurchaseController.class);

    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(30);

        binder.setAllowedFields(
                "token",
                "supplierId",
                "warehouseId",
                "note",
                "items[*].productId",
                "items[*].quantity",
                "items[*].price",
                "items[*].serials"
        );
    }

    @GetMapping({
            "/purchases",
            "/pages/purchases.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        try {
            model.addAttribute(
                    "receipts",
                    service.list(
                            principal.getName(),
                            keyword,
                            supplierId,
                            warehouseId,
                            page
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        model.addAttribute(
                "options",
                service.options(principal.getName())
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("warehouseId", warehouseId);

        return "purchases";
    }

    @GetMapping("/purchases/new")
    public String show(
            HttpSession session,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        String token = (String) session.getAttribute(TOKEN);

        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(TOKEN, token);
        }

        PurchaseRequest form = new PurchaseRequest();
        form.setToken(token);

        model.addAttribute("form", form);

        return builder(principal.getName(), model);
    }

    @PostMapping("/purchases")
    public String create(
            @ModelAttribute("form") PurchaseRequest form,
            BindingResult errors,
            HttpSession session,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        if (errors.hasErrors()) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    "Kiểm tra nhà cung cấp, kho, sản phẩm và số lượng."
            );

            return builder(principal.getName(), model);
        }

        try {
            if (form.getToken() == null
                    || !Objects.equals(
                    session.getAttribute(TOKEN),
                    form.getToken()
            )) {
                var previous = service.existing(
                        principal.getName(),
                        form.getToken()
                );

                if (previous.isPresent()) {
                    return "redirect:/purchases/" + previous.get();
                }

                redirect.addFlashAttribute(
                        "error",
                        "Phiên lập phiếu đã thay đổi. Vui lòng mở lại."
                );

                return "redirect:/purchases/new";
            }

            Long id = service.create(
                    principal.getName(),
                    form
            );

            session.removeAttribute(TOKEN);

            return "redirect:/purchases/" + id;
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot create purchase {}",
                    form.getToken(),
                    exception
            );

            try {
                var previous = service.existing(
                        principal.getName(),
                        form.getToken()
                );

                if (previous.isPresent()) {
                    session.removeAttribute(TOKEN);

                    return "redirect:/purchases/" + previous.get();
                }
            } catch (DataAccessException recovery) {
                log.warn(
                        "Cannot recover purchase {}",
                        form.getToken(),
                        recovery
                );
            }

            response.setStatus(503);

            model.addAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. "
                            + "Giữ nguyên phiếu và kiểm tra lại "
                            + "trước khi thử tiếp."
            );
        }

        return builder(principal.getName(), model);
    }

    @GetMapping("/purchases/{id}")
    public String detail(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "detail",
                service.detail(
                        principal.getName(),
                        id
                )
        );

        return "purchase-detail";
    }

    private String builder(
            String username,
            Model model
    ) {
        model.addAttribute(
                "options",
                service.options(username)
        );

        return "purchase-form";
    }
}