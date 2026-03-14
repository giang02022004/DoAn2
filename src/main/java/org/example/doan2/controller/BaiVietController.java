package org.example.doan2.controller;

import org.example.doan2.entity.BaiViet;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.service.BaiVietService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/admin/articles")
public class BaiVietController {

    @Autowired
    private BaiVietService baiVietService;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    // 1. Hiển thị danh sách Bài viết
    // Endpoint này nhận Keyword (từ khóa) và TrangThai (trạng thái) từ thẻ <form> trên giao diện 
    // để tiến hành lọc bài viết, kết hợp với tính năng Phân trang của Spring Data.
    @GetMapping({"", "/"})
    public String listArticles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "trangThai", required = false, defaultValue = "ALL") String trangThai,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {
        
        int pageSize = 10;
        // Chuyển trang về 0-index cho Spring Data
        Page<BaiViet> pageResult = baiVietService.getFilteredArticles(keyword, trangThai, page - 1, pageSize, "ngayTao");
        
        model.addAttribute("articles", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages() == 0 ? 1 : pageResult.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("activeMenu", "articles");
        
        return "admin/article/list";
    }

    // 2. Hiển thị Form để Admin nhập bài viết mới
    // Truyền vào 1 Object BaiViet rỗng để Spring Data Binding mapping các thuộc tính HTML.
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("article", new BaiViet());
        model.addAttribute("activeMenu", "articles");
        return "admin/article/create";
    }

    // 3. Xử lý logic Thêm mới Bài viết sau khi nhấn nút Submit từ form /admin/articles/create
    // Hàm này sẽ nhặt file ảnh gửi lên, lưu tên ảnh vào Entity, đồng thời gắn NguoiDung (Admin đang đăng nhập) làm Tác Giả.
    @PostMapping("/store")
    public String storeArticle(
            @ModelAttribute("article") BaiViet article,
            @RequestParam("hinhAnhFile") MultipartFile hinhAnhFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Lấy thông tin Tác giả (NguoiDung) đang đăng nhập
            if (authentication != null && authentication.getName() != null) {
                NguoiDung tacGia = nguoiDungRepository.findByEmail(authentication.getName()).orElse(null);
                article.setTacGia(tacGia);
            }

            // Xử lý upload ảnh
            if (hinhAnhFile != null && !hinhAnhFile.isEmpty()) {
                String fileName = saveImage(hinhAnhFile);
                article.setHinhAnh(fileName);
            }

            article.setNgayTao(LocalDateTime.now());
            article.setNgayCapNhat(LocalDateTime.now());
            
            baiVietService.saveArticle(article);
            redirectAttributes.addFlashAttribute("success", "Đã thêm mới bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu bài viết: " + e.getMessage());
        }

        return "redirect:/admin/articles";
    }

    // 4. Hiển thị Form Chỉnh sửa dựa trên ID
    // Gọi Database kiểm tra xem Bài Viết đó có tồn tại không, nếu có thì Load dữ liệu cũ lên giao diện.
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        BaiViet article = baiVietService.getArticleById(id);
        if (article == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy Bài viết.");
            return "redirect:/admin/articles";
        }
        
        model.addAttribute("article", article);
        model.addAttribute("activeMenu", "articles");
        return "admin/article/edit";
    }

    // 5. Xử lý Cập nhật một bài viết hiện có
    // Dữ liệu mới gửi qua Form được ghi đè lên những dữ liệu cũ tương ứng.
    // Nếu Admin không up ảnh mới, hàm sẽ giữ nguyên ảnh cũ.
    @PostMapping("/update")
    public String updateArticle(
            @ModelAttribute("article") BaiViet formArticle,
            @RequestParam("hinhAnhFile") MultipartFile hinhAnhFile,
            RedirectAttributes redirectAttributes) {
        
        try {
            BaiViet existing = baiVietService.getArticleById(formArticle.getId());
            if (existing != null) {
                existing.setTieuDe(formArticle.getTieuDe());
                existing.setNoiDungNgan(formArticle.getNoiDungNgan());
                existing.setNoiDungChiTiet(formArticle.getNoiDungChiTiet());
                existing.setTrangThai(formArticle.getTrangThai());
                existing.setNgayCapNhat(LocalDateTime.now());

                // Nếu có upload ảnh mới
                if (hinhAnhFile != null && !hinhAnhFile.isEmpty()) {
                    String fileName = saveImage(hinhAnhFile);
                    existing.setHinhAnh(fileName);
                }

                baiVietService.saveArticle(existing);
                redirectAttributes.addFlashAttribute("success", "Cập nhật bài viết thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật bài viết: " + e.getMessage());
        }

        return "redirect:/admin/articles";
    }

    // 6. Xử lý Xóa bài viết
    // Hành động này hiện tại đang xóa thật cứng (Hard Delete) khỏi Database.
    @GetMapping("/delete/{id}")
    public String deleteArticle(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            baiVietService.deleteArticle(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa bài viết thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa bài viết này.");
        }
        return "redirect:/admin/articles";
    }

    // Hàm tiện ích hỗ trợ lưu ảnh
    private String saveImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return null;

        // Đổi tên tránh trùng lặp nếu cần, tuy nhiên ở đây giữ lại tên gốc hoặc chèn UUID
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;
        // Xử lý đường dẫn lưu trữ tuyệt đối dựa trên thư mục chạy dự án (tránh lỗi Tomcat Temp Path)
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/img/articles";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        return "articles/" + fileName;
    }
}
