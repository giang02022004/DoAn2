package org.example.doan2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class ProductController {
    @GetMapping("/detail")
    public String productDetail() {
        return "shop-detail"; // shop-detail.html
    }
}
