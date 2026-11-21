package com.example.consolestore.Controller;

import com.example.consolestore.Config.SecurityConfig;
import com.example.consolestore.Service.BundlePricing;
import com.example.consolestore.Service.CartService;
import com.example.consolestore.Service.CartSession;
import com.example.consolestore.Service.CartStore;
import com.example.consolestore.Service.GuestCartCookie;
import com.example.consolestore.Service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.function.Function;

@Controller
public class CartController {

    private final CartService service;
    private final CartSession session;
    private final MemberService members;
    private final CartStore store;

    public CartController(
            CartService service,
            CartSession session,
            MemberService members,
            CartStore store
    ) {
        this.service = service;
        this.session = session;
        this.members = members;
        this.store = store;
    }

    @GetMapping({"/cart", "/pages/cart.html"})
    public String show(
            Authentication authentication,
            HttpServletRequest request,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        CartStore.Owner owner = owner(authentication, request, response, false);
        boolean guest = owner.isGuest();

        return session.locked(owner, store, () -> {
            model.addAttribute("activeStoreMenu", "cart");
            model.addAttribute("bundlePercent", BundlePricing.PERCENT);
            model.addAttribute("bundleOffers", Map.of());
            model.addAttribute("guest", guest);

            try {
                int percent = 0;

                if (!guest) {
                    MemberService.Status member =
                            members.status(authentication.getName());

                    model.addAttribute("member", member);
                    percent = member.tier().getPercent();
                }

                model.addAttribute(
                        "cart",
                        service.view(session.items(), percent)
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
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        return change(
                owner(authentication, request, response, true),
                redirect,
                items -> service.update(items, productId, parse(quantity)),
                "Đã cập nhật số lượng."
        );
    }

    @PostMapping("/cart/items/{productId}/remove")
    public String remove(
            @PathVariable Long productId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        return change(
                owner(authentication, request, response, true),
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
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        CartStore.Owner owner = owner(authentication, request, response, true);

        String result = change(
                owner,
                redirect,
                items -> service.add(items, productId, parse(quantity)),
                "Đã thêm sản phẩm vào giỏ hàng."
        );

        return "checkout".equals(next) && !owner.isGuest()
                ? "redirect:/checkout"
                : result;
    }

    @GetMapping("/cart/count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> count(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CartStore.Owner owner = owner(authentication, request, response, false);

        long quantity = store.load(owner)
                .values()
                .stream()
                .mapToLong(Integer::longValue)
                .sum();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("quantity", quantity));
    }

    // Khách hàng -> giỏ theo tài khoản; còn lại -> giỏ theo cookie CART_ID
    private CartStore.Owner owner(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean createIfMissing
    ) {
        if (authentication != null
                && "KHACHHANG".equals(SecurityConfig.roleOf(authentication))) {
            return store.customerOwner(authentication.getName());
        }

        String token = GuestCartCookie.read(request);

        if (token == null && createIfMissing) {
            token = GuestCartCookie.issue(request, response);
        }

        return CartStore.Owner.guest(token);
    }

    private String change(
            CartStore.Owner owner,
            RedirectAttributes redirect,
            Function<Map<Long, Integer>, Map<Long, Integer>> action,
            String message
    ) {
        try {
            session.locked(owner, store, () -> {
                session.requireEditable();

                session.replaceItems(
                        action.apply(session.items())
                );

                return null;
            });

            redirect.addFlashAttribute("cartSuccess", message);
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("cartError", exception.getMessage());
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