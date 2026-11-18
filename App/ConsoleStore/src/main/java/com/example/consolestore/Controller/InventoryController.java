package com.example.consolestore.Controller;

import com.example.consolestore.DTO.InventoryFilter;
import com.example.consolestore.Service.InventoryService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

@Controller
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @InitBinder("filter")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields(
                "keyword",
                "warehouseId",
                "condition",
                "availability",
                "type",
                "from",
                "to"
        );
    }

    @ModelAttribute("movementLabels")
    public Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();

        labels.put("NHAP", "Nhập hàng");
        labels.put("BAN", "Bán hàng");
        labels.put("THU_CU", "Thu mua cũ");
        labels.put("HOAN_HUY", "Hoàn do hủy đơn");

        return labels;
    }

    @GetMapping({
            "/inventory",
            "/pages/inventory.html"
    })
    public String list(
            @ModelAttribute("filter") InventoryFilter filter,
            BindingResult errors,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        validate(errors);

        model.addAttribute(
                "stocks",
                service.list(
                        principal.getName(),
                        filter,
                        page
                )
        );

        model.addAttribute(
                "warehouses",
                service.warehouses(principal.getName())
        );

        return "inventory";
    }

    @GetMapping("/inventory/products/{id}")
    public String history(
            @PathVariable Long id,
            @ModelAttribute("filter") InventoryFilter filter,
            BindingResult errors,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        validate(errors);

        model.addAttribute(
                "history",
                service.history(
                        principal.getName(),
                        id,
                        filter,
                        page
                )
        );

        model.addAttribute(
                "warehouses",
                service.warehouses(principal.getName())
        );

        return "inventory-history";
    }

    private void validate(BindingResult errors) {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kho hoặc ngày lọc không hợp lệ."
            );
        }
    }
}