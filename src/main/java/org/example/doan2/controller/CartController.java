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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Hiển thị trang giỏ hàng (Cart Page).
     * Lấy danh sách sản phẩm và tổng tiền để đưa vào Model cho Thymeleaf hiển thị.
     */
    @GetMapping
    public String viewCart(HttpSession session, Model model, java.security.Principal principal) {
        // Principal: Lấy thông tin người dùng đang đăng nhập thông qua Spring Security
        String email = principal != null ? principal.getName() : null;
        
        // Lấy danh sách sản phẩm trong giỏ (tự động phân biệt Session khách hoặc DB User)
        List<CartItem> cart = cartService.getCart(session, email);
        
        model.addAttribute("cartItems", cart);
        model.addAttribute("totalPrice", cartService.getTotalPrice(session, email));
        
        return "cart";
    }

    /**
     * Xử lý yêu cầu thêm sản phẩm vào giỏ hàng từ trang danh sách hoặc chi tiết.
     * Hỗ trợ xử lý biến thể (RAM/CPU/Màu sắc) và tính toán giá khuyến mãi ngay lập tức.
     */
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId, 
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false) Integer variantId,
                            HttpSession session,
                            java.security.Principal principal,
                            RedirectAttributes redirectAttributes) {
        String email = principal != null ? principal.getName() : null;
        SanPham sp = sanPhamService.getSanPhamById(productId);
        
        if (sp != null) {
            Integer price = sp.getGia();
            String variantInfo = null;

            // 1. Tính giá dựa trên cấu hình biến thể (nếu có)
            if (variantId != null) {
                BienTheSanPham bienThe = bienTheSanPhamRepository.findById(variantId).orElse(null);
                if (bienThe != null) {
                    // Giá cuối = Giá gốc + Giá cộng thêm của cấu hình
                    price = sp.getGia() + (bienThe.getGiaThem() != null ? bienThe.getGiaThem() : 0);
                    variantInfo = bienThe.getCpu() + " / " + bienThe.getBoNho() + " / " + bienThe.getMauSac();
                }
            }

            // 2. Kiểm tra và áp dụng giá khuyến mãi trực tiếp vào item giỏ hàng
            if (sp.isDangKhuyenMai()) {
                int phanTramGiam = sp.getKhuyenMai().getPhanTramGiam();
                price = price - (price * phanTramGiam / 100);
            }

            // 3. Đóng gói dữ liệu vào DTO CartItem
            CartItem item = new CartItem(sp.getId(), sp.getTenSanPham(), price, quantity, sp.getHinhAnh(), variantId, variantInfo);
            
            try {
                // Gọi service để thực hiện lưu vào DB hoặc Session
                cartService.addToCart(session, email, item);
            } catch (RuntimeException e) {
                // Xử lý lỗi nghiệp vụ (ví dụ: Hết hàng) và báo lại cho người dùng
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/shop-detail/" + productId;
            }
        }
        // Sau khi thêm thành công, chuyển hướng người dùng đến trang giỏ hàng
        return "redirect:/cart";
    }
    
    /**
     * Xóa một mặt hàng khỏi giỏ hàng.
     */
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id,
                                 @RequestParam(required = false) Integer variantId,
                                 HttpSession session,
                                 java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        cartService.removeFromCart(session, email, id, variantId);
        return "redirect:/cart";
    }

    /**
     * Cập nhật số lượng sản phẩm ngay tại trang giỏ hàng.
     * Sử dụng AJAX (@ResponseBody) để cập nhật UI mượt mà không cần load lại trang.
     * 
     * @return Map dữ liệu JSON chứa trạng thái thành công, tổng tiền mới, và số lượng Badge giỏ hàng.
     */
    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> updateQuantity(@RequestParam Integer productId,
                                               @RequestParam(required = false) Integer variantId,
                                               @RequestParam int quantity,
                                               HttpSession session,
                                               java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Thực hiện cập nhật số lượng (có kiểm tra tồn kho)
            cartService.updateQuantity(session, email, productId, variantId, quantity);
            
            // 2. Lấy lại dữ liệu giỏ hàng mới nhất sau khi cập nhật
            List<CartItem> cart = cartService.getCart(session, email);
            CartItem currentItem = cart.stream()
                    .filter(item -> item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId))
                    .findFirst()
                    .orElse(null);
            
            // 3. Phản hồi các thông số cần thiết để Frontend cập nhật DOM
            if (currentItem != null) {
                // Tổng tiền của duy nhất dòng sản phẩm đó (Giá x Số lượng)
                response.put("itemTotal", currentItem.getTotalPrice());
            }
            // Tổng tiền của cả giỏ hàng
            response.put("totalPrice", cartService.getTotalPrice(session, email));
            // Tổng số lượng item để cập nhật Badge Icon ở Header
            response.put("cartCount", cartService.getCount(session, email));
            
            response.put("success", true);
        } catch (RuntimeException e) {
            // Trả về thông báo lỗi (ví dụ: "Số lượng vượt quá tồn kho")
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
}
