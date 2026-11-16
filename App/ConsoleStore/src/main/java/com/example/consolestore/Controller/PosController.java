package com.example.consolestore.Controller;

import com.example.consolestore.Dto.*;
import com.example.consolestore.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class PosController {

    private static final Logger log =
            LoggerFactory.getLogger(PosController.class);

    private final PosService service;
    private final PosSession session;

    public PosController(
            PosService service,
            PosSession session
    ) {
        this.service = service;
        this.session = session;
    }

    @InitBinder("form")
    public void bind(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(30);

        binder.setAllowedFields(
                "warehouseId",
                "phone",
                "newCustomer",
                "fullName",
                "address",
                "tradeInId",
                "note",
                "lines[*].productId",
                "lines[*].serialId",
                "lines[*].quantity"
        );
    }

    @InitBinder("payment")
    public void bindPayment(WebDataBinder binder) {
        binder.setAllowedFields(
                "method",
                "cashReceived",
                "confirmed"
        );
    }

    @GetMapping({"/pos", "/pages/pos.html"})
    public String show(
            @RequestParam(required = false) Long warehouseId,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return session.locked(principal.getName(), () -> {
            if (session.submitted()) {
                return "redirect:/pos/review";
            }

            PosRequest form = session.quote() == null
                    ? new PosRequest()
                    : PosRequest.from(session.quote());

            if (warehouseId != null
                    && !warehouseId.equals(
                    form.getWarehouseId()
            )) {
                form = new PosRequest();
                form.setWarehouseId(warehouseId);
            }

            model.addAttribute("form", form);

            return builder(
                    principal.getName(),
                    form,
                    model,
                    response
            );
        });
    }

    @PostMapping("/pos/preview")
    public String preview(
            @ModelAttribute("form") PosRequest form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        return session.locked(principal.getName(), () -> {
            if (session.submitted()) {
                redirect.addFlashAttribute(
                        "posError",
                        "Yêu cầu trước chưa rõ kết quả."
                );

                return "redirect:/pos/review";
            }

            if (errors.hasErrors()) {
                response.setStatus(400);

                model.addAttribute(
                        "posError",
                        "Kiểm tra lại mã sản phẩm, "
                                + "số lượng, serial và phiếu thu."
                );

                return builder(
                        principal.getName(),
                        form,
                        model,
                        response
                );
            }

            try {
                session.start(
                        service.preview(
                                principal.getName(),
                                form
                        )
                );

                return "redirect:/pos/review";
            } catch (PosRejectedException exception) {
                response.setStatus(400);

                model.addAttribute(
                        "posError",
                        exception.getMessage()
                );
            }

            return builder(
                    principal.getName(),
                    form,
                    model,
                    response
            );
        });
    }

    @GetMapping("/pos/review")
    public String review(
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return session.locked(
                principal.getName(),
                () -> reviewView(
                        principal.getName(),
                        model,
                        response
                )
        );
    }

    @PostMapping("/pos/complete")
    public String complete(
            @RequestParam String requestId,
            @ModelAttribute("payment") PosPaymentRequest payment,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect
    ) {
        response.setHeader("Cache-Control", "no-store");

        return session.locked(principal.getName(), () -> {
            var quote = session.quote();

            if (quote == null
                    || !quote.requestId().equals(requestId)) {
                var found = service.existing(
                        principal.getName(),
                        requestId
                );

                if (found.isPresent()) {
                    return receiptUrl(found.get());
                }

                redirect.addFlashAttribute(
                        "posError",
                        "Phiên bán hàng đã thay đổi. "
                                + "Hãy lập lại bản xem trước."
                );

                return "redirect:/pos";
            }

            if (errors.hasErrors()) {
                response.setStatus(400);

                model.addAttribute(
                        "posError",
                        "Thông tin thanh toán không hợp lệ."
                );

                return reviewView(
                        principal.getName(),
                        model,
                        response
                );
            }

            session.markSubmitted();

            try {
                Long id = service.place(
                        principal.getName(),
                        quote,
                        payment
                );

                session.complete();

                return receiptUrl(id);
            } catch (PosRejectedException exception) {
                session.rejected();

                redirect.addFlashAttribute(
                        "posError",
                        exception.getMessage()
                );

                return "redirect:/pos";
            } catch (AccessDeniedException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.error(
                        "Cannot confirm POS request {}",
                        requestId,
                        exception
                );

                try {
                    var found = service.existing(
                            principal.getName(),
                            requestId
                    );

                    if (found.isPresent()) {
                        session.complete();
                        return receiptUrl(found.get());
                    }
                } catch (AccessDeniedException denied) {
                    throw denied;
                } catch (RuntimeException recovery) {
                    log.warn(
                            "POS recovery failed {}",
                            requestId,
                            recovery
                    );
                }

                response.setStatus(503);

                model.addAttribute(
                        "posError",
                        "Chưa xác định kết quả. "
                                + "Gửi lại cùng yêu cầu; "
                                + "không lập thêm hóa đơn thay thế."
                );

                return reviewView(
                        principal.getName(),
                        model,
                        response
                );
            }
        });
    }

    @GetMapping("/pos/invoices/{id}")
    public String receipt(
            @PathVariable Long id,
            Principal principal,
            Model model,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        model.addAttribute(
                "receipt",
                service.receipt(
                        principal.getName(),
                        id
                )
        );

        return "pos-receipt";
    }

    private String reviewView(
            String username,
            Model model,
            HttpServletResponse response
    ) {
        var quote = session.quote();

        if (quote == null) {
            return "redirect:/pos";
        }

        try {
            var found = service.existing(
                    username,
                    quote.requestId()
            );

            if (found.isPresent()) {
                session.complete();
                return receiptUrl(found.get());
            }
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            response.setStatus(503);

            log.warn(
                    "Cannot check POS request {}",
                    quote.requestId(),
                    exception
            );

            model.addAttribute(
                    "posError",
                    "Chưa kiểm tra được kết quả. "
                            + "Giữ nguyên mã yêu cầu và thử lại."
            );
        }

        model.addAttribute("quote", quote);
        model.addAttribute(
                "pending",
                session.submitted()
        );

        if (!model.containsAttribute("payment")) {
            model.addAttribute(
                    "payment",
                    new PosPaymentRequest()
            );
        }

        return "pos-review";
    }

    private String builder(
            String username,
            PosRequest form,
            Model model,
            HttpServletResponse response
    ) {
        try {
            var options = service.options(
                    username,
                    form.getWarehouseId()
            );

            form.setWarehouseId(options.warehouseId());
            model.addAttribute("options", options);
        } catch (PosRejectedException exception) {
            response.setStatus(400);

            model.addAttribute(
                    "posError",
                    exception.getMessage()
            );

            var options = service.options(
                    username,
                    null
            );

            form.setWarehouseId(options.warehouseId());
            model.addAttribute("options", options);
        }

        return "pos";
    }

    private static String receiptUrl(Long id) {
        return "redirect:/pos/invoices/" + id;
    }
}