package com.example.consolestore.Controller;

import com.example.consolestore.Dto.*;
import com.example.consolestore.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class CheckoutController {

    private static final Logger log =
            LoggerFactory.getLogger(CheckoutController.class);

    private final CheckoutService checkout;
    private final CartService carts;
    private final CartSession session;

    public CheckoutController(
            CheckoutService checkout,
            CartService carts,
            CartSession session
    ) {
        this.checkout = checkout;
        this.carts = carts;
        this.session = session;
    }

    @GetMapping({
            "/checkout",
            "/pages/checkout.html"
    })
    public String show(
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        noCache(response);

        return session.locked(principal.getName(), () -> {
            CheckoutQuote quote = session.currentQuote();

            if (quote != null && quote.submitted()) {
                try {
                    Optional<Long> existing =
                            checkout.findExisting(
                                    principal.getName(),
                                    quote.requestId()
                            );

                    if (existing.isPresent()) {
                        session.complete(
                                quote.requestId(),
                                existing.get()
                        );

                        return redirect(existing.get());
                    }
                } catch (AccessDeniedException exception) {
                    throw exception;
                } catch (RuntimeException exception) {
                    log.warn(
                            "Cannot recover checkout {}",
                            quote.requestId(),
                            exception
                    );
                }

                model.addAttribute(
                        "checkoutError",
                        "Yêu cầu trước chưa xác định kết quả. "
                                + "Gửi lại sẽ dùng cùng mã yêu cầu."
                );
            }

            return render(
                    principal.getName(),
                    null,
                    model,
                    response
            );
        });
    }

    @PostMapping("/checkout")
    public String place(
            @RequestParam String requestId,
            @ModelAttribute("form") CheckoutRequest form,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        noCache(response);

        return session.locked(principal.getName(), () -> {
            Long completed = session.completed(requestId);

            if (completed != null) {
                return redirect(completed);
            }

            CheckoutQuote quote = session.currentQuote();

            if (quote == null
                    || !quote.requestId().equals(requestId)) {
                Optional<Long> existing =
                        checkout.findExisting(
                                principal.getName(),
                                requestId
                        );

                if (existing.isPresent()) {
                    return redirect(existing.get());
                }

                response.setStatus(409);

                model.addAttribute(
                        "checkoutError",
                        "Phiên thanh toán đã thay đổi. "
                                + "Kiểm tra giỏ hàng và gửi lại."
                );

                return render(
                        principal.getName(),
                        form,
                        model,
                        response
                );
            }

            session.markSubmitted();

            try {
                Long invoiceId = checkout.place(
                        principal.getName(),
                        quote,
                        form
                );

                session.complete(
                        requestId,
                        invoiceId
                );

                return redirect(invoiceId);
            } catch (CheckoutRejectedException exception) {
                session.rejectAttempt();
                response.setStatus(409);

                model.addAttribute(
                        "checkoutError",
                        exception.getMessage()
                );
            } catch (AccessDeniedException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.error(
                        "Checkout failed, request {}",
                        requestId,
                        exception
                );

                try {
                    Optional<Long> existing =
                            checkout.findExisting(
                                    principal.getName(),
                                    requestId
                            );

                    if (existing.isPresent()) {
                        session.complete(
                                requestId,
                                existing.get()
                        );

                        return redirect(existing.get());
                    }
                } catch (AccessDeniedException denied) {
                    throw denied;
                } catch (RuntimeException recoveryError) {
                    log.warn(
                            "Checkout recovery failed, request {}",
                            requestId,
                            recoveryError
                    );
                }

                response.setStatus(503);

                model.addAttribute(
                        "checkoutError",
                        "Chưa xác định kết quả đặt hàng. "
                                + "Giữ nguyên phiên và gửi lại; "
                                + "hệ thống sử dụng cùng mã yêu cầu "
                                + "để tránh tạo trùng."
                );
            }

            return render(
                    principal.getName(),
                    form,
                    model,
                    response
            );
        });
    }

    @GetMapping("/checkout/orders/{id}/payment")
    public String payment(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        noCache(response);

        model.addAttribute("activeStoreMenu", "cart");

        model.addAttribute(
                "page",
                checkout.payment(
                        principal.getName(),
                        id
                )
        );

        return "payments";
    }

    private String render(
            String username,
            CheckoutRequest form,
            Model model,
            HttpServletResponse response
    ) {
        model.addAttribute("activeStoreMenu", "cart");
        model.addAttribute("requestId", "");
        model.addAttribute("cart", null);
        model.addAttribute("pending", false);

        model.addAttribute(
                "form",
                form == null ? new CheckoutRequest() : form
        );

        try {
            if (form == null) {
                model.addAttribute(
                        "form",
                        checkout.defaults(username)
                );
            }

            CartView cart = carts.view(session.items());
            model.addAttribute("cart", cart);

            if (cart.readyToCheckout()) {
                Map<Long, BigDecimal> prices =
                        new LinkedHashMap<>();

                cart.lines().forEach(
                        line -> prices.put(
                                line.productId(),
                                line.price()
                        )
                );

                CheckoutQuote quote =
                        session.prepareQuote(prices);

                model.addAttribute(
                        "requestId",
                        quote.requestId()
                );

                model.addAttribute(
                        "pending",
                        quote.submitted()
                );
            } else if (session.currentQuote() != null
                    && session.currentQuote().submitted()) {
                model.addAttribute(
                        "requestId",
                        session.currentQuote().requestId()
                );

                model.addAttribute("pending", true);
            }
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            response.setStatus(503);

            log.warn(
                    "Cannot render checkout",
                    exception
            );

            model.addAttribute(
                    "checkoutError",
                    "Chưa tải được thông tin thanh toán. "
                            + "Vui lòng tải lại trang."
            );
        }

        return "checkout";
    }

    private static String redirect(Long invoiceId) {
        return "redirect:/checkout/orders/"
                + invoiceId
                + "/payment";
    }

    private static void noCache(
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store"
        );
    }
}