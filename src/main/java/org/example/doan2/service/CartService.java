package org.example.doan2.service;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CartItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CartService {
    private static final String CART_SESSION_KEY = "cart";

    // Lấy giỏ hàng từ session
    public List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    // So sánh 2 cart item: cùng productId VÀ cùng variantId
    private boolean isSameItem(CartItem a, CartItem b) {
        if (!a.getId().equals(b.getId())) return false;
        return Objects.equals(a.getVariantId(), b.getVariantId());
    }

    // Thêm sản phẩm vào giỏ
    public void addToCart(HttpSession session, CartItem item) {
        List<CartItem> cart = getCart(session);
        boolean exists = false;
        for (CartItem cartItem : cart) {
            if (isSameItem(cartItem, item)) {
                cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
                exists = true;
                break;
            }
        }
        if (!exists) {
            cart.add(item);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    // Xóa sản phẩm khỏi giỏ (theo productId + variantId)
    public void removeFromCart(HttpSession session, Integer productId, Integer variantId) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(item -> item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId));
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    // Cập nhật số lượng (theo productId + variantId)
    public void updateQuantity(HttpSession session, Integer productId, Integer variantId, int quantity) {
        List<CartItem> cart = getCart(session);
        for (CartItem item : cart) {
            if (item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId)) {
                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    // Xóa hết giỏ hàng
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }
    
    // Tính tổng tiền
    public Integer getTotalPrice(HttpSession session) {
        List<CartItem> cart = getCart(session);
        return cart.stream().mapToInt(CartItem::getTotalPrice).sum();
    }
    
    // Đếm số lượng sản phẩm
    public int getCount(HttpSession session) {
        List<CartItem> cart = getCart(session);
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
