package org.example.doan2.controller;
import org.example.doan2.service.HangSanXuatService;
import org.example.doan2.service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

    private final SanPhamService sanPhamService;
    private final HangSanXuatService hangSanXuatService;

    public IndexController(SanPhamService sanPhamService,
                           HangSanXuatService hangSanXuatService) {
        this.sanPhamService = sanPhamService;
        this.hangSanXuatService = hangSanXuatService;
    }

    // Trang index ban đầu
    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("hangLaptop",
                hangSanXuatService.getHangByLoai("LAPTOP"));

        model.addAttribute("dsLaptop",
                sanPhamService.getTop15ByLoai("LAPTOP"));

        return "index";
    }

    // API load laptop theo hãng (KHÔNG reload)
    @GetMapping("/index/laptop")
    public String loadLaptopByHang(
            @RequestParam(required = false) Integer hangId,
            Model model
    ) {
        String loai = "LAPTOP";

        if (hangId == null) {
            model.addAttribute("dsLaptop",
                    sanPhamService.getTop15ByLoai(loai));
        } else {
            model.addAttribute("dsLaptop",
                    sanPhamService.getTop15ByLoaiAndHang(loai, hangId));
        }


        return "fragments/laptop-list :: laptopList";
    }
}


