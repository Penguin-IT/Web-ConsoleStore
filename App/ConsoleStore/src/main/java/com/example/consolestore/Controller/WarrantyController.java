package com.example.consolestore.Controller;

import com.example.consolestore.DTO.WarrantyRequest;
import com.example.consolestore.Service.WarrantyService;

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
import java.util.*;

@Controller
public class WarrantyController {

    private static final String TOKEN = "warrantyToken";

    private static final Logger log =
            LoggerFactory.getLogger(WarrantyController.class);

    private final WarrantyService service;

    public WarrantyController(WarrantyService service) {
        this.service = service;
    }

    @ModelAttribute("statusLabels")
    public Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put("TIEP_NHAN", "Tiếp nhận");
        labels.put("DANG_XU_LY", "Đang xử lý");
        labels.put("HOAN_TAT", "Hoàn tất");

        return labels;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields(
                "token",
                "serial",
                "description",
                "dueAt",
                "notes",
                "cost",
                "action",
                "expectedStatus"
        );
    }

    @GetMapping({
            "/warranties",
            "/pages/warranties.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "cases",
                service.list(
                        principal.getName(),
                        keyword,
                        status,
                        page
                )
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "warranties";
    }

    @GetMapping("/warranties/new")
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

        WarrantyRequest form = new WarrantyRequest();
        form.setToken(token);

        model.addAttribute("form", form);

        return builder(form, principal.getName(), model);
    }

    @PostMapping("/warranties/lookup")
    public String lookup(
            @ModelAttribute("form") WarrantyRequest form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        if (errors.hasErrors()) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    "Kiểm tra serial và ngày hẹn trả."
            );
        }

        return builder(form, principal.getName(), model);
    }

    @PostMapping("/warranties")
    public String receive(
            @ModelAttribute("form") WarrantyRequest form,
            BindingResult errors,
            Principal principal,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        if (errors.hasErrors()) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    "Thông tin tiếp nhận không hợp lệ."
            );

            return builder(form, principal.getName(), model);
        }

        try {
            if (form.getToken() == null
                    || !Objects.equals(
                    session.getAttribute(TOKEN),
                    form.getToken()
            )) {
                var existing = service.existing(
                        principal.getName(),
                        form.getToken()
                );

                if (existing.isPresent()) {
                    return "redirect:/warranties/" + existing.get();
                }

                redirect.addFlashAttribute(
                        "error",
                        "Phiên tiếp nhận đã thay đổi. Vui lòng mở lại."
                );

                return "redirect:/warranties/new";
            }

            Long id = service.receive(
                    principal.getName(),
                    form
            );

            session.removeAttribute(TOKEN);

            return "redirect:/warranties/" + id;
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot receive warranty {}",
                    form.getToken(),
                    exception
            );

            try {
                var existing = service.existing(
                        principal.getName(),
                        form.getToken()
                );

                if (existing.isPresent()) {
                    session.removeAttribute(TOKEN);

                    return "redirect:/warranties/" + existing.get();
                }
            } catch (DataAccessException recovery) {
                log.warn(
                        "Cannot recover warranty {}",
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

    @GetMapping("/warranties/{id}")
    public String detail(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        var detail = service.detail(
                principal.getName(),
                id
        );

        WarrantyRequest form = new WarrantyRequest();

        form.setNotes(detail.notes());
        form.setCost(detail.receipt().getCost().toPlainString());
        form.setDueAt(detail.receipt().getDueAt());
        form.setExpectedStatus(detail.receipt().getStatus());

        model.addAttribute("detail", detail);
        model.addAttribute("form", form);

        return "warranty-detail";
    }

    @PostMapping("/warranties/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("form") WarrantyRequest form,
            BindingResult errors,
            Principal principal,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute(
                    "error",
                    "Ngày hẹn trả hoặc dữ liệu xử lý không hợp lệ."
            );

            return "redirect:/warranties/" + id;
        }

        try {
            service.update(
                    principal.getName(),
                    id,
                    form
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã cập nhật phiếu bảo hành."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot update warranty {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. Vui lòng tải lại phiếu."
            );
        }

        return "redirect:/warranties/" + id;
    }

    private String builder(
            WarrantyRequest form,
            String username,
            Model model
    ) {
        var lookup = service.lookup(
                username,
                form.getSerial()
        );

        model.addAttribute("lookup", lookup);

        if (lookup != null && form.getDueAt() == null) {
            form.setDueAt(
                    lookup.getServerNow()
                            .plusDays(7)
                            .withSecond(0)
                            .withNano(0)
            );
        }

        return "warranty-form";
    }
}