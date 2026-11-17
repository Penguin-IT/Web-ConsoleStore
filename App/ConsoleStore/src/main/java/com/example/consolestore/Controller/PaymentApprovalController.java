package com.example.consolestore.Controller;

import com.example.consolestore.DTO.PaymentReviewRequest;
import com.example.consolestore.Service.PaymentApprovalService;

import jakarta.servlet.http.HttpServletResponse;

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
public class PaymentApprovalController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentApprovalController.class
            );

    private final PaymentApprovalService service;

    public PaymentApprovalController(
            PaymentApprovalService service
    ) {
        this.service = service;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields(
                "action",
                "expectedMethod",
                "expectedAmount",
                "receivedAmount",
                "received"
        );
    }

    @ModelAttribute("methodLabels")
    public Map<String, String> methods() {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put("CHUYEN_KHOAN", "Ngân hàng");
        labels.put("MOMO", "MoMo");
        labels.put("ZALOPAY", "ZaloPay");

        return labels;
    }

    @ModelAttribute("orderLabels")
    public Map<String, String> statuses() {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put("CHO_XAC_NHAN", "Chờ xác nhận");
        labels.put("DA_XAC_NHAN", "Đã xác nhận");
        labels.put("DANG_GIAO", "Đang giao");
        labels.put("HOAN_TAT", "Hoàn tất");
        labels.put("HUY", "Đã hủy");

        return labels;
    }

    @GetMapping({
            "/payments",
            "/pages/payments.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String method,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "payments",
                service.list(
                        principal.getName(),
                        keyword,
                        method,
                        page
                )
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("method", method);

        return "payment-approvals";
    }

    @GetMapping("/payments/{id}")
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

        return "payment-review";
    }

    @PostMapping("/payments/{id}/review")
    public String review(
            @PathVariable Long id,
            @ModelAttribute("form") PaymentReviewRequest form,
            BindingResult errors,
            Principal principal,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute(
                    "error",
                    "Thông tin duyệt thanh toán không hợp lệ."
            );

            return "redirect:/payments/" + id;
        }

        try {
            service.review(
                    principal.getName(),
                    id,
                    form
            );

            redirect.addFlashAttribute(
                    "success",
                    "approve".equals(form.getAction())
                            ? "Đã xác nhận thanh toán và đơn hàng."
                            : "Đơn đã được hủy."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot review payment {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa xác nhận được kết quả. "
                            + "Hãy kiểm tra lại trạng thái trước khi thử lại."
            );
        }

        return "redirect:/payments/" + id;
    }
}