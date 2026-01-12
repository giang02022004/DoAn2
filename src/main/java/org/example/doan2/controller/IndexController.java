package org.example.doan2.controller;
import org.example.doan2.service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
class IndexController {
    private final SanPhamService sanPhamService;
    public IndexController(SanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("dsLaptop", this.sanPhamService.getLaptop());
        model.addAttribute("dsPhuKien", this.sanPhamService.getPhuKien());
        return "index";
    }
}
