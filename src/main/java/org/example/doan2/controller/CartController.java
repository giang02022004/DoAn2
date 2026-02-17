package org.example.doan2.controller;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CartItem;
import org.example.doan2.entity.SanPham;
import org.example.doan2.service.CartService;
import org.example.doan2.service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final SanPhamService sanPhamService;

    public CartController(CartService cartService, SanPhamService sanPhamService) {
        this.cartService = cartService;
        this.sanPhamService = sanPhamService;
    }

    // Hiển thị giỏ hàng
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItem> cart = cartService.getCart(session);
        model.addAttribute("cartItems", cart);
        model.addAttribute("totalPrice", cartService.getTotalPrice(session));
        return "cart";
    }

    // Thêm vào giỏ hàng
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId, 
                            @RequestParam(defaultValue = "1") int quantity, 
                            HttpSession session) {
        SanPham sp = sanPhamService.getSanPhamById(productId);
        if (sp != null) {
            CartItem item = new CartItem(sp.getId(), sp.getTenSanPham(), sp.getGia(), quantity, sp.getHinhAnh());
            cartService.addToCart(session, item);
        }
        return "redirect:/cart";
    }
    
    // Xóa sản phẩm
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id, HttpSession session) {
        cartService.removeFromCart(session, id);
        return "redirect:/cart";
    }

    // Cập nhật số lượng (AJAX hoặc Form)
    @PostMapping("/update")
    @ResponseBody
    public String updateQuantity(@RequestParam Integer productId, @RequestParam int quantity, HttpSession session) {
        cartService.updateQuantity(session, productId, quantity);
        return "ok";
    }
}


