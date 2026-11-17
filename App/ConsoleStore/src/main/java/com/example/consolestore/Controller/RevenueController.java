package com.example.consolestore.Controller;

import com.example.consolestore.Service.RevenueService;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Controller
public class RevenueController {

    private static final Logger log =
            LoggerFactory.getLogger(RevenueController.class);

    private final RevenueService service;

    public RevenueController(RevenueService service) {
        this.service = service;
    }

    @GetMapping({
            "/revenue",
            "/pages/revenue.html"
    })
    public String report(
            @RequestParam(defaultValue = "month") String mode,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "day") String grouping,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        String selectedMonth = month == null
                ? YearMonth.from(today).toString()
                : month;

        String selectedFrom = from == null
                ? today.withDayOfMonth(1).toString()
                : from;

        String selectedTo = to == null
                ? today.toString()
                : to;

        model.addAttribute("mode", mode);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("from", selectedFrom);
        model.addAttribute("to", selectedTo);
        model.addAttribute("grouping", grouping);

        try {
            model.addAttribute(
                    "report",
                    service.report(
                            principal.getName(),
                            mode,
                            selectedMonth,
                            selectedFrom,
                            selectedTo,
                            grouping
                    )
            );
        } catch (IllegalArgumentException exception) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            log.warn("Cannot load revenue report", exception);

            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );

            model.addAttribute(
                    "error",
                    "Tạm thời chưa tải được báo cáo. "
                            + "Vui lòng thử lại."
            );
        }

        return "revenue";
    }
}