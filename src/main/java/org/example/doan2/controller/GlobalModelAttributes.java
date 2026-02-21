package org.example.doan2.controller;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.service.CartService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// @ControllerAdvice: Đánh dấu class này là nơi xử lý chung cho TẤT CẢ các Controller trong ứng dụng.
// Bất kỳ model nào được thêm ở đây sẽ tự động có mặt trong mọi trang HTML (Thymeleaf).
@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;

    public GlobalModelAttributes(CartService cartService) {
        this.cartService = cartService;
    }

    // @ModelAttribute("cartCount"): Tự động thêm thuộc tính "cartCount" vào Model của mọi request.
    // Giúp hiển thị số lượng sản phẩm trong giỏ hàng trên Header (icon giỏ hàng) ở TẤT CẢ các trang
    // mà không cần phải thêm thủ công trong từng Controller.
    @ModelAttribute("cartCount")
    public int getCartCount(HttpSession session) {
        return cartService.getCount(session);
    }
}
