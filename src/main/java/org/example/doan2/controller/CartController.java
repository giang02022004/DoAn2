package org.example.doan2.controller;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CartItem;
import org.example.doan2.entity.BienTheSanPham;
import org.example.doan2.entity.SanPham;
import org.example.doan2.repository.BienTheSanPhamRepository;
import org.example.doan2.service.CartService;
import org.example.doan2.service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final SanPhamService sanPhamService;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    public CartController(CartService cartService, SanPhamService sanPhamService,
                          BienTheSanPhamRepository bienTheSanPhamRepository) {
        this.cartService = cartService;
        this.sanPhamService = sanPhamService;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
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
                            @RequestParam(required = false) Integer variantId,
                            HttpSession session) {
        SanPham sp = sanPhamService.getSanPhamById(productId);
        if (sp != null) {
            Integer price = sp.getGia();
            String variantInfo = null;

            if (variantId != null) {
                BienTheSanPham bienThe = bienTheSanPhamRepository.findById(variantId).orElse(null);
                if (bienThe != null) {
                    price = sp.getGia() + (bienThe.getGiaThem() != null ? bienThe.getGiaThem() : 0);
                    variantInfo = bienThe.getCpu() + " / " + bienThe.getBoNho() + " / " + bienThe.getMauSac();
                }
            }

            CartItem item = new CartItem(sp.getId(), sp.getTenSanPham(), price, quantity, sp.getHinhAnh(), variantId, variantInfo);
            cartService.addToCart(session, item);
        }
        return "redirect:/cart";
    }
    
    // Xóa sản phẩm
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id,
                                 @RequestParam(required = false) Integer variantId,
                                 HttpSession session) {
        cartService.removeFromCart(session, id, variantId);
        return "redirect:/cart";
    }

    // Cập nhật số lượng (AJAX)
    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> updateQuantity(@RequestParam Integer productId,
                                               @RequestParam(required = false) Integer variantId,
                                               @RequestParam int quantity,
                                               HttpSession session) {
        cartService.updateQuantity(session, productId, variantId, quantity);
        
        List<CartItem> cart = cartService.getCart(session);
        CartItem currentItem = cart.stream()
                .filter(item -> item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId))
                .findFirst()
                .orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        if (currentItem != null) {
            response.put("itemTotal", currentItem.getTotalPrice());
        }
        response.put("totalPrice", cartService.getTotalPrice(session));
        response.put("cartCount", cartService.getCount(session));
        return response;
    }
}
