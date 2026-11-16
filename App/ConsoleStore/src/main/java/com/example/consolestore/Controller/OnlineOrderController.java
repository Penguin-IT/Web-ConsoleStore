package com.example.consolestore.Controller;

import com.example.consolestore.Dto.OnlineOrderData;
import com.example.consolestore.Service.OnlineOrderService;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class OnlineOrderController {

    private static final Logger log =
            LoggerFactory.getLogger(OnlineOrderController.class);

    private final OnlineOrderService service;

    public OnlineOrderController(OnlineOrderService service) {
        this.service = service;
    }

    @ModelAttribute("statusLabels")
    public Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();

        for (String status : new String[]{
                "CHO_XAC_NHAN",
                "DA_XAC_NHAN",
                "DANG_GIAO",
                "HOAN_TAT",
                "HUY",
                "DA_THANH_TOAN",
                "CHUA_THANH_TOAN"
        }) {
            labels.put(
                    status,
                    OnlineOrderData.statusLabel(status)
            );
        }

        return labels;
    }

    @GetMapping({
            "/online-orders",
            "/pages/online-orders.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "orders",
                service.list(
                        principal.getName(),
                        status,
                        keyword,
                        page
                )
        );

        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "online-orders";
    }

    @GetMapping("/online-orders/{id}")
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

        return "online-order-detail";
    }

    @PostMapping("/online-orders/{id}/status")
    public String transition(
            @PathVariable Long id,
            @RequestParam String action,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            service.transition(
                    principal.getName(),
                    id,
                    action
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã cập nhật đơn hàng."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot change online order {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa thể xác nhận kết quả. "
                            + "Hãy kiểm tra trạng thái đơn trước khi thử lại."
            );
        }

        return "redirect:/online-orders/" + id;
    }

    @PostMapping("/online-orders/{id}/serial")
    public String assign(
            @PathVariable Long id,
            @RequestParam Long lineId,
            @RequestParam Long serialId,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            service.assign(
                    principal.getName(),
                    id,
                    lineId,
                    serialId
            );

            redirect.addFlashAttribute(
                    "success",
                    "Đã gán serial."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn(
                    "Cannot assign serial for online order {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "error",
                    "Chưa thể gán serial. Vui lòng kiểm tra lại đơn hàng."
            );
        }

        return "redirect:/online-orders/" + id;
    }
}