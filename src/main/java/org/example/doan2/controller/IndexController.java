package org.example.doan2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
class IndexController {
    @GetMapping("/index")
    public String home() {
        return "index"; // index.html trong templates
    }
}
