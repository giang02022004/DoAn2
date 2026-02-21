package org.example.doan2.controller;

import org.example.doan2.entity.BienTheSanPham;
import org.example.doan2.entity.SanPham;
import org.example.doan2.service.HangSanXuatService;
import org.example.doan2.service.SanPhamService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
@Controller
public class ShopController {

    private final SanPhamService sanPhamService;
    private final HangSanXuatService hangSanXuatService;

    public ShopController(SanPhamService sanPhamService, HangSanXuatService hangSanXuatService) {
        this.sanPhamService = sanPhamService;
        this.hangSanXuatService = hangSanXuatService;
    }

    // Trang cửa hàng (Shop): Hiển thị danh sách sản phẩm, hỗ trợ lọc theo Hãng, Loại và Giá
    @GetMapping("/shop")
    public String shop(Model model, 
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Integer hangId,
                       @RequestParam(required = false) String loai) {
        // 9 sản phẩm mỗi trang, mặc định sắp xếp mới nhất
        Pageable pageable = PageRequest.of(page, 9, Sort.by(Sort.Direction.DESC, "id"));
        Page<SanPham> sanPhamPage;



        // Default maxPrice to null/very high if not needed, or handle in service
        Integer maxPrice = null; // Or logic to get maxPrice from request if you want to support ?maxPrice=... on initial load

        // Unified Search for initial load as well
        sanPhamPage = sanPhamService.searchSanPhamsCombined(null, hangId, loai, maxPrice, pageable);
        
        // Pass params back to view to keep state if needed (optional)
        if (hangId != null) model.addAttribute("currentHangId", hangId);
        if (loai != null) model.addAttribute("currentLoai", loai);

        model.addAttribute("dsSanPham", sanPhamPage);
        
        // Load danh mục hãng
        model.addAttribute("hangLaptops", hangSanXuatService.getHangByLoai("LAPTOP"));
        model.addAttribute("hangPhuKiens", hangSanXuatService.getHangByLoai("PHU_KIEN"));

        return "shop";
    }

    // API: Lấy danh sách sản phẩm (AJAX) hỗ trợ phân trang và lọc (Hãng, Loại, Giá)
    @GetMapping("/api/shop/products")
    @ResponseBody
    public ResponseEntity<Page<SanPham>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer hangId,
            @RequestParam(required = false) String loai,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id_desc") String sort) {

        // Xử lý logic sắp xếp
        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        switch (sort) {
            case "price_asc":
                sortObj = Sort.by(Sort.Direction.ASC, "gia");
                break;
            case "price_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "gia");
                break;
            case "name_asc":
                sortObj = Sort.by(Sort.Direction.ASC, "tenSanPham");
                break;
            default:
                sortObj = Sort.by(Sort.Direction.DESC, "id");
                break;
        }

        Pageable pageable = PageRequest.of(page, 9, sortObj);
        Page<SanPham> sanPhamPage;

        // Xử lý maxPrice: Frontend gửi 0 khi không chọn, đổi thành null để query bỏ qua
        Integer effectiveMaxPrice = (maxPrice != null && maxPrice > 0) ? maxPrice : null;

        // Sử dụng phương thức tìm kiếm kết hợp (Keyword + Hãng + Loại + Giá + Sắp xếp)
        sanPhamPage = sanPhamService.searchSanPhamsCombined(keyword, hangId, loai, effectiveMaxPrice, pageable);

        return ResponseEntity.ok(sanPhamPage);
    }

    // Trang chi tiết sản phẩm
    @GetMapping("/shop-detail/{id}")
    public String shopDetail(@PathVariable Integer id, Model model) {
        SanPham sanPham = sanPhamService.getSanPhamById(id);
        if (sanPham == null) {
            return "redirect:/shop";
        }
        model.addAttribute("sanPham", sanPham);
        
        // Load biến thể sản phẩm
        List<BienTheSanPham> bienTheList = sanPham.getBienTheList();
        model.addAttribute("bienTheList", bienTheList != null ? bienTheList : java.util.Collections.emptyList());
        
        // Sản phẩm liên quan
        List<SanPham> relatedProducts = sanPhamService.getRelatedProducts(
                sanPham.getLoaiSanPham().getId(),
                sanPham.getId(),
                6
        );
        model.addAttribute("relatedProducts", relatedProducts);
        
        return "shop-detail";
    }
}
