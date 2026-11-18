package com.example.consolestore.Controller;

import com.example.consolestore.Service.InvoiceService;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
public class InvoiceController {

    private static final Logger log =
            LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping({
            "/invoices",
            "/pages/invoices.html"
    })
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String channel,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute("keyword", keyword);
        model.addAttribute("channel", channel);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);

        try {
            model.addAttribute(
                    "invoices",
                    service.list(
                            principal.getName(),
                            keyword,
                            channel,
                            status,
                            from,
                            to,
                            sort,
                            page
                    )
            );
        } catch (IllegalArgumentException exception) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            model.addAttribute("error", exception.getMessage());
        } catch (DataAccessException exception) {
            log.warn("Cannot load invoices", exception);

            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );

            model.addAttribute(
                    "error",
                    "Tạm thời chưa tải được hóa đơn."
            );
        }

        return "invoices";
    }

    @GetMapping("/invoices/{id}")
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

        return "invoice-detail";
    }
}