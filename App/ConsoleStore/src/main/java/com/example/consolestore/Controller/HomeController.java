package com.example.consolestore.controller;

import com.example.consolestore.service.HomePageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomePageService homePageService;

    public HomeController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute(
                "products",
                homePageService.getFeaturedProducts()
        );
        return "home";
    }
}