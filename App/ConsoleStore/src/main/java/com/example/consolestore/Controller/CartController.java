package com.example.consolestore.Controller;

import com.example.consolestore.Service.BundlePricing;
import com.example.consolestore.Service.CartService;
import com.example.consolestore.Service.CartSession;
import com.example.consolestore.Service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;
import java.util.function.Function;

@Controller
public class CartController {

    private final CartService service;
    private final CartSession session;

    private final MemberService members;

    public CartController(
            CartService service,
            CartSession session,
            MemberService members
    ) {
        this.service = service;
        this.session = session;
        this.members = members;
    }

    @GetMapping({"/cart", "/pages/cart.html"})
    public String show(
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return session.locked(principal.getName(), () -> {
            model.addAttribute("activeStoreMenu", "cart");
            model.addAttribute("bundlePercent", BundlePricing.PERCENT);
            model.addAttribute("bundleOffers", Map.of());

            try {
                MemberService.Status member = members.status(principal.getName());
                model.addAttribute("member", member);

                model.addAttribute(
                        "cart",
                        service.view(session.items(), member.tier().getPercent())
                );

                model.addAttribute(
                        "bundleOffers",
                        service.bundleOffersByConsole(session.items(), 3)
                );
            } catch (DataAccessException exception) {
                response.setStatus(503);

                model.addAttribute("cart", null);
                model.addAttribute(
                        "cartError",
                        "Chưa tải được giỏ hàng. Vui lòng thử lại."
                );
            }

            return "cart";
        });
    }



    @PostMapping("/cart/items/{productId}/quantity")
    public String update(
            @PathVariable Long productId,
            @RequestParam String quantity,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return change(
                principal,
                redirect,
                items -> service.update(
                        items,
                        productId,
                        parse(quantity)
                ),
                "Đã cập nhật số lượng."
        );
    }

    @PostMapping("/cart/items/{productId}/remove")
    public String remove(
            @PathVariable Long productId,
            Principal principal,
            RedirectAttributes redirect
    ) {
        return change(
                principal,
                redirect,
                items -> service.remove(items, productId),
                "Đã xóa sản phẩm khỏi giỏ hàng."
        );
    }
    @PostMapping("/cart/items")
    public String add(
            @RequestParam Long productId,
            @RequestParam String quantity,
            @RequestParam(required = false) String next,
            Principal principal,
            RedirectAttributes redirect
    ) {
        String result = change(
                principal,
                redirect,
                items -> service.add(items, productId, parse(quantity)),
                "Đã thêm sản phẩm vào giỏ hàng."
        );

        return "checkout".equals(next) ? "redirect:/checkout" : result;
    }
    @GetMapping("/cart/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> count(
            Principal principal
    ) {
        long quantity = session.locked(
                principal.getName(),
                () -> session.items()
                        .values()
                        .stream()
                        .mapToLong(Integer::longValue)
                        .sum()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("quantity", quantity));
    }

    private String change(
            Principal principal,
            RedirectAttributes redirect,
            Function<Map<Long, Integer>, Map<Long, Integer>> action,
            String message
    ) {
        try {
            session.locked(principal.getName(), () -> {
                session.requireEditable();

                session.replaceItems(
                        action.apply(session.items())
                );

                return null;
            });

            redirect.addFlashAttribute(
                    "cartSuccess",
                    message
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "cartError",
                    exception.getMessage()
            );
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute(
                    "cartError",
                    "Chưa kiểm tra được tồn kho. Giỏ hàng chưa thay đổi."
            );
        }

        return "redirect:/cart";
    }

    private int parse(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Số lượng phải là số nguyên hợp lệ."
            );
        }
    }
}