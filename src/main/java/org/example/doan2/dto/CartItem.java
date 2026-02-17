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
    
    // Tổng tiền của item này
    public Integer getTotalPrice() {
        return price * quantity;
    }
}
