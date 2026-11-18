package com.example.consolestore.Controller;

import com.example.consolestore.DTO.TradeInRequest;
import com.example.consolestore.Service.TradeInService;

import jakarta.servlet.http.*;

import org.slf4j.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
public class TradeInController {

    private static final String TOKEN = "tradeInToken";

    private static final Logger log =
            LoggerFactory.getLogger(TradeInController.class);

    private final TradeInService service;

    public TradeInController(TradeInService service) {
        this.service = service;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(30);

        binder.setAllowedFields(
                "token",
                "phone",
                "newCustomer",
                "fullName",
                "address",
                "warehouseId",
                "note",
                "paid",
                "items[*].productId",
                "items[*].quantity",
                "items[*].price",
                "items[*].serial",
                "items[*].condition",
                "items[*].note"
        );
    }

    @GetMapping({
            "/trade-ins",
            "/pages/trade-ins.html"
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
                "receipts",
                service.list(
                        principal.getName(),
                        keyword,
                        page
                )
        );

        model.addAttribute("keyword", keyword);

        return "trade-ins";
    }

    @GetMapping("/trade-ins/new")
    public String show(
            @RequestParam(defaultValue = "") String phone,
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

        TradeInRequest form = new TradeInRequest();
        form.setToken(token);
        form.setPhone(phone);

        model.addAttribute("form", form);

        return builder(form, principal.getName(), model);
    }

    @PostMapping("/trade-ins")
    public String create(
            @ModelAttribute("form") TradeInRequest form,
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
                    "Kiểm tra lại sản phẩm, số lượng và kho nhận."
            );

            return builder(form, principal.getName(), model);
        }

        try {
            if (!java.util.Objects.equals(
                    session.getAttribute(TOKEN),
                    form.getToken()
            ) || form.getToken() == null) {
                var previous = service.existing(
                        principal.getName(),
                        form.getToken()
                );

                if (previous.isPresent()) {
                    return "redirect:/trade-ins/" + previous.get();
                }

                redirect.addFlashAttribute(
                        "error",
                        "Phiên lập phiếu đã thay đổi. Vui lòng mở lại."
                );

                return "redirect:/trade-ins/new";
            }

            Long id = service.create(
                    principal.getName(),
                    form
            );

            session.removeAttribute(TOKEN);

            return "redirect:/trade-ins/" + id;
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot create trade-in receipt {}",
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

                    return "redirect:/trade-ins/" + previous.get();
                }
            } catch (DataAccessException recovery) {
                log.warn(
                        "Cannot recover trade-in receipt {}",
                        form.getToken(),
                        recovery
                );
            }

            response.setStatus(503);

            model.addAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. "
                            + "Giữ nguyên phiếu và thử lại."
            );
        }

        return builder(form, principal.getName(), model);
    }

    @GetMapping("/trade-ins/{id}")
    public String detail(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "detail",
                service.detail(principal.getName(), id)
        );

        return "trade-in-detail";
    }

    private String builder(
            TradeInRequest form,
            String username,
            Model model
    ) {
        model.addAttribute(
                "options",
                service.options(username, form.getPhone())
        );

        return "trade-in-form";
    }
    @PostMapping("/trade-ins/customer")
    public String lookupCustomer(
            @ModelAttribute("form") TradeInRequest form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        form.setPaid(false);

        String phone = form.getPhone() == null
                ? ""
                : form.getPhone().strip();

        form.setPhone(phone);

        if (errors.hasErrors() || !phone.matches("0[0-9]{9}")) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    "Nhập số điện thoại gồm 10 chữ số, bắt đầu bằng 0."
            );

            return builder(
                    form,
                    principal.getName(),
                    model
            );
        }

        var options = service.options(
                principal.getName(),
                phone
        );

        boolean wasNewCustomer = form.isNewCustomer();

        form.setNewCustomer(options.customer() == null);

        if (!form.isNewCustomer() || !wasNewCustomer) {
            form.setFullName("");
            form.setAddress("");
        }

        model.addAttribute("options", options);

        return "trade-in-form";
    }
}