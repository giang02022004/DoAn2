package org.example.doan2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.example.doan2.service.CartService;
import org.example.doan2.service.BaiVietService;
import org.example.doan2.entity.BaiViet;

@Controller
public class PageController {

    private final CartService cartService;
    private final BaiVietService baiVietService;

    public PageController(CartService cartService, BaiVietService baiVietService) {
        this.cartService = cartService;
        this.baiVietService = baiVietService;
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
    public String testimonial(Model model, HttpSession session, java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        model.addAttribute("cartCount", cartService.getCart(session, email).size());
        
        // Cắm dữ liệu thật từ bảng BaiViet vào giao diện Tin Tức
        model.addAttribute("articles", baiVietService.getFilteredArticles(null, "ACTIVE", 0, 20, "ngayTao").getContent());
        
        return "testimonial";
    }

    @GetMapping("/testimonial/{id}")
    public String articleDetail(@PathVariable("id") Integer id, Model model, HttpSession session, java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        model.addAttribute("cartCount", cartService.getCart(session, email).size());
        
        BaiViet article = baiVietService.getArticleById(id);
        if (article == null || !"ACTIVE".equals(article.getTrangThai())) {
            return "redirect:/404"; // Chuyển hướng nếu bài viết không tồn tại hoặc không ở trạng thái ACTIVE
        }
        
        model.addAttribute("article", article);
        return "article-detail";
    }

    @GetMapping("/404")
    public String error404() {
        return "error/404";
    }

}
