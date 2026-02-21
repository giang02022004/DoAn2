package org.example.doan2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Integer id;
    private String name;
    private Integer price;
    private int quantity;
    private String image;
    private Integer variantId;    // null nếu không có biến thể (phụ kiện)
    private String variantInfo;   // mô tả biến thể, vd: "i7 / 16GB / Đen"
    
    // Constructor cũ (tương thích ngược cho phụ kiện)
    public CartItem(Integer id, String name, Integer price, int quantity, String image) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.image = image;
        this.variantId = null;
        this.variantInfo = null;
    }

    // Tổng tiền của item này
    public Integer getTotalPrice() {
        return price * quantity;
    }
}
