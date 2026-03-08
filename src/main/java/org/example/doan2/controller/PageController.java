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
    public String checkout(HttpSession session, Model model, java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        var cartItems = cartService.getCart(session, email);
        
        // [BẢO MẬT/LOGIC] Lớp 1: Kiểm tra giỏ hàng trống.
        // Ngăn chặn người dùng cố tình gõ URL /checkout khi giỏ hàng chưa có sản phẩm.
        // Nếu giỏ hàng trống, tự động đá ngược về trang /cart để xem giỏ.
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", cartService.getTotalPrice(session, email));
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
