package org.example.doan2.controller;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CheckoutDTO;
import org.example.doan2.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place-order")
    public String placeOrder(@ModelAttribute CheckoutDTO checkoutDTO, HttpSession session, RedirectAttributes redirectAttributes, java.security.Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            orderService.placeOrder(checkoutDTO, session, principal.getName());
            return "redirect:/checkout/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Order failed: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
    
    @RequestMapping("/success")
    public String orderSuccess() {
        return "order-success";
    }
}
