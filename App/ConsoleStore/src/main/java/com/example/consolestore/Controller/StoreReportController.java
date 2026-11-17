package com.example.consolestore.Controller;

import com.example.consolestore.Service.StoreReportService;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.*;

@Controller
public class StoreReportController {

    private static final Logger log =
            LoggerFactory.getLogger(StoreReportController.class);

    private final StoreReportService service;

    public StoreReportController(StoreReportService service) {
        this.service = service;
    }

    @GetMapping({
            "/purchase-costs",
            "/pages/purchase-costs.html"
    })
    public String purchaseCosts(
            @RequestParam(required = false) String fromMonth,
            @RequestParam(required = false) String toMonth,
            @RequestParam(defaultValue = "") String supplierId,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        YearMonth current = YearMonth.from(today());

        String start = fromMonth == null
                ? current.minusMonths(5).toString()
                : fromMonth;

        String end = toMonth == null
                ? current.toString()
                : toMonth;

        model.addAttribute("fromMonth", start);
        model.addAttribute("toMonth", end);
        model.addAttribute("supplierId", supplierId);

        try {
            model.addAttribute(
                    "suppliers",
                    service.supplierOptions(principal.getName())
            );

            model.addAttribute(
                    "report",
                    service.purchaseCosts(
                            principal.getName(),
                            start,
                            end,
                            supplierId
                    )
            );
        } catch (IllegalArgumentException exception) {
            badRequest(response, model, exception);
        } catch (DataAccessException exception) {
            unavailable(response, model, exception);
        }

        return "purchase-costs";
    }

    @GetMapping({
            "/stock-reports",
            "/pages/stock-reports.html"
    })
    public String stockReports(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") String top,
            @RequestParam(defaultValue = "5") String threshold,
            @RequestParam(defaultValue = "0") String page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        LocalDate today = today();

        String start = from == null
                ? today.minusDays(29).toString()
                : from;

        String end = to == null
                ? today.toString()
                : to;

        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("top", top);
        model.addAttribute("threshold", threshold);

        try {
            model.addAttribute(
                    "report",
                    service.stockReport(
                            principal.getName(),
                            start,
                            end,
                            top,
                            threshold,
                            page
                    )
            );
        } catch (IllegalArgumentException exception) {
            badRequest(response, model, exception);
        } catch (DataAccessException exception) {
            unavailable(response, model, exception);
        }

        return "stock-reports";
    }

    private LocalDate today() {
        return LocalDate.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
    }

    private void badRequest(
            HttpServletResponse response,
            Model model,
            IllegalArgumentException exception
    ) {
        response.setStatus(
                HttpServletResponse.SC_BAD_REQUEST
        );

        model.addAttribute(
                "error",
                exception.getMessage()
        );
    }

    private void unavailable(
            HttpServletResponse response,
            Model model,
            DataAccessException exception
    ) {
        log.warn("Cannot load store report", exception);

        response.setStatus(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE
        );

        model.addAttribute(
                "error",
                "Tạm thời chưa tải được báo cáo. Vui lòng thử lại."
        );
    }
}