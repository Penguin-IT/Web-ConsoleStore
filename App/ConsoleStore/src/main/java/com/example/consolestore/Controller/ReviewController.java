package com.example.consolestore.Controller;

import com.example.consolestore.Service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ReviewController {

    private static final Logger log =
            LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/products/{id}/reviews")
    public String submit(
            @PathVariable Long id,
            @RequestParam(required = false) Integer stars,
            @RequestParam(defaultValue = "") String content,
            Principal principal,
            RedirectAttributes redirect
    ) {
        try {
            service.submit(principal.getName(), id, stars, content);

            redirect.addFlashAttribute(
                    "reviewSuccess",
                    "Cảm ơn bạn! Đánh giá đã được đăng."
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "reviewError",
                    exception.getMessage()
            );
            redirect.addFlashAttribute("reviewDraft", content);
        } catch (AccessDeniedException exception) {
            redirect.addFlashAttribute(
                    "reviewError",
                    "Chỉ tài khoản khách hàng mới được đánh giá."
            );
        } catch (ResponseStatusException exception) {
            return "redirect:/products";
        } catch (DataIntegrityViolationException exception) {
            redirect.addFlashAttribute(
                    "reviewError",
                    "Bạn đã đánh giá sản phẩm này rồi."
            );
        } catch (DataAccessException exception) {
            log.warn("Cannot save review for product {}", id, exception);

            redirect.addFlashAttribute(
                    "reviewError",
                    "Chưa gửi được đánh giá. Vui lòng thử lại sau."
            );
        }

        return "redirect:/products/" + id + "#reviews";
    }
}