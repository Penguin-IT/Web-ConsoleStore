package com.example.consolestore.Controller;

import com.example.consolestore.Service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.dao.DataAccessException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {
    private static final Logger log=LoggerFactory.getLogger(HomeController.class);
    private final CatalogService catalog;
    public HomeController(CatalogService catalog){this.catalog=catalog;}
    @GetMapping({"/","/home"})
    public String home(Model model,HttpServletResponse response) {
        try {
            model.addAttribute("consoles",catalog.featured("Máy console",4));
            model.addAttribute("accessories",catalog.featured("Tay cầm",2));
            model.addAttribute(
                    "games",
                    catalog.featured("Đĩa game", 4)
            );

            model.addAttribute("activeStoreMenu", "home");
            return "home";
        } catch(DataAccessException exception) {
            log.error("Cannot read home products: {}",exception.getClass().getSimpleName());
            response.setStatus(503);
            model.addAttribute("message","Chưa thể tải sản phẩm. Vui lòng thử lại sau.");
            return "error/catalog-unavailable";
        }
    }
    @GetMapping("/home.html")
    public String previousHomeUrl(){return "redirect:/";}
}
