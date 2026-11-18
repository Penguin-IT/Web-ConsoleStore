package com.example.consolestore.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String showHomePage(Model model) {
        model.addAttribute("consoles", List.of());
        model.addAttribute("accessories", List.of());
        model.addAttribute("games", List.of());

        return "home";
    }
}