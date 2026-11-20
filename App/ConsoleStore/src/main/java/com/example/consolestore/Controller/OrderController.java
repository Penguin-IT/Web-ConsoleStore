package com.example.consolestore.Controller;

import com.example.consolestore.Service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class OrderController {

    private static final Logger log =
            LoggerFactory.getLogger(OrderController.class);

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping({
            "/orders",
            "/pages/orders.html"
    })
    public String show(
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
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

        model.addAttribute("orders", null);

        try {
            model.addAttribute(
                    "orders",
                    service.list(
                            principal.getName(),
                            page
                    )
            );
        } catch (
                DataAccessException
                | TransactionException exception
        ) {
            response.setStatus(503);

            log.error(
                    "Cannot load customer orders",
                    exception
            );

            model.addAttribute(
                    "orderError",
                    "Chưa tải được đơn hàng. "
                            + "Vui lòng tải lại trang."
            );
        }

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute("activeStoreMenu", "account");

        try {
            var detail = service.detail(principal.getName(), id);
            model.addAttribute("detail", detail);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (DataAccessException | TransactionException exception) {
            response.setStatus(503);
            log.error("Cannot load order detail {}", id, exception);
            model.addAttribute("orderError", "Chưa tải được đơn hàng. Vui lòng tải lại trang.");
        }

        return "order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            service.cancel(
                    principal.getName(),
                    id
            );

            redirect.addFlashAttribute(
                    "orderSuccess",
                    "Đơn đã hủy. Tồn kho đã được hoàn lại."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "orderError",
                    exception.getMessage()
            );
        } catch (
                AccessDeniedException
                | ResponseStatusException exception
        ) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "Cannot complete cancellation for order {}",
                    id,
                    exception
            );

            redirect.addFlashAttribute(
                    "orderError",
                    "Chưa xác định được kết quả hủy. "
                            + "Kiểm tra lại trạng thái trước khi thử lại; "
                            + "không đặt thêm đơn để thay thế."
            );
        }

        return "redirect:/orders";
    }
}
