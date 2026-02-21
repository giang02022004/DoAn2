package org.example.doan2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final org.example.doan2.service.CartService cartService;

    public PageController(org.example.doan2.service.CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session,Model model) {
        model.addAttribute("cartItems", cartService.getCart(session));
        model.addAttribute("totalPrice", cartService.getTotalPrice(session));
        return "checkout";
    }

    @GetMapping("/testimonial")
    public String testimonial() {
        return "testimonial";
    }

    @GetMapping("/404")
    public String error404() {
        return "404";
    }

}
