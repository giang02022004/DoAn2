package org.example.doan2.controller;

import org.example.doan2.entity.KhuyenMai;
import org.example.doan2.service.KhuyenMaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
public class KhuyenMaiController {

    @Autowired
    private KhuyenMaiService khuyenMaiService;

    @GetMapping
    public String listPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        int pageSize = 10;
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<KhuyenMai> khuyenMaiPage = khuyenMaiService.getKhuyenMais(keyword, pageable);

        model.addAttribute("khuyenMaiPage", khuyenMaiPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", khuyenMaiPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeMenu", "promotions");
        
        return "admin/promotion/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("khuyenMai", new KhuyenMai());
        model.addAttribute("activeMenu", "promotions");
        return "admin/promotion/create";
    }

    @PostMapping("/create")
    public String saveKhuyenMai(@ModelAttribute KhuyenMai khuyenMai, RedirectAttributes redirectAttributes) {
        khuyenMaiService.saveKhuyenMai(khuyenMai);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm chương trình khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        KhuyenMai khuyenMai = khuyenMaiService.getKhuyenMaiById(id);
        if (khuyenMai == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy chương trình khuyến mãi!");
            return "redirect:/admin/promotions";
        }
        model.addAttribute("khuyenMai", khuyenMai);
        model.addAttribute("activeMenu", "promotions");
        return "admin/promotion/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateKhuyenMai(@PathVariable Integer id, @ModelAttribute KhuyenMai khuyenMai, RedirectAttributes redirectAttributes) {
        khuyenMai.setId(id);
        KhuyenMai existing = khuyenMaiService.getKhuyenMaiById(id);
        if (existing != null) {
            if (khuyenMai.getTrangThai() == null) {
                 khuyenMai.setTrangThai(existing.getTrangThai());
            }
        }
        khuyenMaiService.saveKhuyenMai(khuyenMai);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/delete/{id}")
    public String deleteKhuyenMai(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        khuyenMaiService.deleteKhuyenMai(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa (ẩn) khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }
}
