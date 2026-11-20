package com.example.consolestore.Controller;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Service.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
public class CatalogController {
    private static final Logger log=LoggerFactory.getLogger(CatalogController.class);
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog){this.catalog=catalog;}
    public record NavigationItem(String label,String url,boolean active) {}
    @GetMapping({"/products","/catalog.html"})
    public String list(@ModelAttribute("filter") CatalogSearch filter, BindingResult errors, Model model, HttpServletResponse response) {
        try {
            catalog.validate(filter,errors);
            model.addAttribute("filterErrors",filterMessages(errors));
            Page<ProductCard> result=errors.hasErrors()?Page.empty():catalog.search(filter);
            model.addAttribute("result",result);
            var categories=catalog.categories();var platforms=catalog.platforms();
            model.addAttribute("categories",categories);model.addAttribute("platforms",platforms);
            model.addAttribute("activeStoreMenu",categories.stream().filter(c->Objects.equals(c.getId(),filter.getCategoryId())).map(c->c.getName().equals("Đĩa game")?"games":c.getName().equals("Tay cầm")?"controllers":"products").findFirst().orElse("products"));
            List<NavigationItem> categoryLinks=new ArrayList<>(),platformLinks=new ArrayList<>(),pages=new ArrayList<>();
            for(var c:categories)categoryLinks.add(new NavigationItem(c.getName(),url(filter,"categoryId",c.getId()),Objects.equals(c.getId(),filter.getCategoryId())||c.getName().equals(filter.getCategory())));
            for(var p:platforms)platformLinks.add(new NavigationItem(p.getName(),url(filter,"platformId",p.getId()),Objects.equals(p.getId(),filter.getPlatformId())||p.getName().equals(filter.getPlatform())));
            if(!errors.hasErrors())for(int i=Math.max(0,result.getNumber()-2);i<Math.min(result.getTotalPages(),result.getNumber()+3);i++)
                pages.add(new NavigationItem(String.valueOf(i+1),url(filter,"page",i),i==result.getNumber()));
            model.addAttribute("categoryLinks",categoryLinks);model.addAttribute("platformLinks",platformLinks);model.addAttribute("pageLinks",pages);
            model.addAttribute("previousUrl",url(filter,"page",Math.max(0,result.getNumber()-1)));
            model.addAttribute("nextUrl",url(filter,"page",result.getNumber()+1));
            return "catalog";
        } catch(DataAccessException exception) {
            return databaseError(exception,model,response);
        }
    }
    // JSON uses the same search service as the HTML page.
    public record ProductPage(List<ProductCard> items,int page,int totalPages,long totalElements) {}

    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<?> data(@ModelAttribute("filter") CatalogSearch filter, BindingResult errors) {
        try {
            catalog.validate(filter,errors);
            if(errors.hasErrors())
                return ResponseEntity.badRequest().body(Map.of("message","Bộ lọc không hợp lệ."));
            var result=catalog.search(filter);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new ProductPage(result.getContent(),result.getNumber(),result.getTotalPages(),result.getTotalElements()));
        } catch(DataAccessException exception) {
            log.error("Cannot read products: {}",exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message","Chưa thể tải dữ liệu sản phẩm. Vui lòng thử lại sau."));
        }
    }

    // Field conversion failures are displayed beside the corresponding HTML input.
    private Map<String,String> filterMessages(BindingResult errors) {
        Map<String,String> messages=new LinkedHashMap<>();
        for(var error:errors.getFieldErrors()) {
            String message=error.getDefaultMessage();
            if(error.isBindingFailure()) {
                message=switch(error.getField()) {
                    case "minPrice" -> "Giá từ phải là một số hợp lệ.";
                    case "maxPrice" -> "Giá đến phải là một số hợp lệ.";
                    case "categoryId" -> "Loại sản phẩm không hợp lệ.";
                    case "platformId" -> "Nền tảng không hợp lệ.";
                    case "page" -> "Số trang không hợp lệ.";
                    case "size" -> "Số sản phẩm mỗi trang không hợp lệ.";
                    default -> "Giá trị không hợp lệ.";
                };
            }
            messages.putIfAbsent(error.getField(),Objects.requireNonNullElse(message,"Thông tin không hợp lệ."));
        }
        return messages;
    }

    private String databaseError(DataAccessException exception,Model model,HttpServletResponse response) {
        log.error("Cannot read products: {}",exception.getClass().getSimpleName());
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("message","Chưa thể tải sản phẩm. Vui lòng thử lại sau.");
        return "error/catalog-unavailable";

    }
    @GetMapping({
            "/products/{id}",
            "/pages/product-detail.html"
    })
    public String detail(
            @PathVariable(name = "id", required = false) String pathId,
            @RequestParam(name = "id", required = false) String queryId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        model.addAttribute("activeStoreMenu", "products");

        String value = pathId != null ? pathId : queryId;

        if (value == null || value.isBlank()) {
            return "redirect:/products";
        }

        long id;

        try {
            id = Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return productNotFound(model, response);
        }

        try {
            var result = catalog.detail(id);

            if (result.isEmpty()) {
                return productNotFound(model, response);
            }

            var detail = result.get();

            model.addAttribute("product", detail.product());
            model.addAttribute("description", detail.description());
            model.addAttribute("related", catalog.related(id, 4));

            // CN04 sẽ cung cấp tài khoản và quyền từ phía server.
            model.addAttribute(
                    "authenticated",
                    request.getUserPrincipal() != null
            );

            model.addAttribute(
                    "customer",
                    request.getUserPrincipal() != null
                            && request.isUserInRole("KHACHHANG")
            );



            return "product-detail";
        } catch (DataAccessException exception) {
            return databaseError(exception, model, response);
        }
    }

    private String productNotFound(
            Model model,
            HttpServletResponse response
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());

        model.addAttribute(
                "message",
                "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh."
        );

        return "product-detail";
    }
    private String url(CatalogSearch f, String changed, Object value) {
        Map<String,Object> params=new LinkedHashMap<>();
        params.put("q",f.getQ());params.put("categoryId",f.getCategoryId());params.put("platformId",f.getPlatformId());
        params.put("category",f.getCategory());params.put("platform",f.getPlatform());params.put("condition",f.getCondition());
        params.put("minPrice",f.getMinPrice());params.put("maxPrice",f.getMaxPrice());params.put("sort",f.getSort());params.put("size",f.getSize());params.put("page",0);
        if(changed.equals("categoryId"))params.remove("category");if(changed.equals("platformId"))params.remove("platform");params.put(changed,value);
        UriComponentsBuilder builder=UriComponentsBuilder.fromPath("/products");
        params.forEach((k,v)->{if(v!=null&&!v.toString().isBlank())builder.queryParam(k,v);});
        return builder.build().encode().toUriString();
    }
}
