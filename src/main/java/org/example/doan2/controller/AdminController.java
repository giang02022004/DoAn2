package org.example.doan2.controller;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.doan2.repository.DonHangRepository;
import org.example.doan2.repository.HangSanXuatRepository;
import org.example.doan2.repository.LoaiSanPhamRepository;
import org.example.doan2.repository.SanPhamRepository;
import org.example.doan2.entity.SanPham;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Tiêm các Repository để lấy dữ liệu thống kê từ database
    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final HangSanXuatRepository hangSanXuatRepository;
    private final LoaiSanPhamRepository loaiSanPhamRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(NguoiDungRepository nguoiDungRepository,
                           DonHangRepository donHangRepository,
                           SanPhamRepository sanPhamRepository,
                           HangSanXuatRepository hangSanXuatRepository,
                           LoaiSanPhamRepository loaiSanPhamRepository,
                           PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.donHangRepository = donHangRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.hangSanXuatRepository = hangSanXuatRepository;
        this.loaiSanPhamRepository = loaiSanPhamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        
        // Lấy dữ liệu thống kê tổng quan đưa ra model để hiển thị trên Admin Dashboard (Thymeleaf: admin/index.html)
        model.addAttribute("totalOrders", donHangRepository.count()); // Tổng số lượng đơn hàng
        model.addAttribute("totalRevenue", donHangRepository.sumTotalRevenue()); // Tổng doanh thu
        model.addAttribute("totalProducts", sanPhamRepository.count()); // Tổng số sản phẩm trong kho
        model.addAttribute("totalCustomers", nguoiDungRepository.count()); // Tổng số tài khoản khách hàng
        
        // Lấy 5 đơn hàng mới nhất để hiển thị vào bảng "Đơn Hàng Mới"
        model.addAttribute("recentOrders", donHangRepository.findTop5ByOrderByNgayTaoDesc());
        
        // ─── Thống kê Biểu Đồ Theo Tháng ───
        // Truy vấn dữ liệu doanh thu và số đơn theo tháng từ Database
        List<Object[]> revenueData = donHangRepository.getMonthlyRevenue();
        List<Object[]> orderCountData = donHangRepository.getMonthlyOrderCount();
        
        // Dùng Map để tra cứu nhanh: key = "yyyy-M" (VD: "2025-3"), value = số liệu
        Map<String, Long> revenueByMonth = new LinkedHashMap<>();
        Map<String, Long> ordersByMonth = new LinkedHashMap<>();
        
        // Nạp dữ liệu doanh thu vào Map
        for (Object[] row : revenueData) {
            String key = row[0] + "-" + row[1]; // key = "năm-tháng"
            revenueByMonth.put(key, ((Number) row[2]).longValue());
        }
        // Nạp dữ liệu số đơn vào Map
        for (Object[] row : orderCountData) {
            String key = row[0] + "-" + row[1];
            ordersByMonth.put(key, ((Number) row[2]).longValue());
        }
        
        // Xây dựng danh sách 12 tháng gần nhất (tính từ tháng hiện tại ngược về quá khứ)
        // Dùng List thực để Thymeleaf tự động serialize thành JS Array đúng định dạng
        LocalDate now = LocalDate.now();
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartRevenues = new ArrayList<>();
        List<Long> chartOrderCounts = new ArrayList<>();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            int year = date.getYear();
            int month = date.getMonthValue();
            String key = year + "-" + month;
            // Nhãn hiển thị: "Th.3/2025"
            chartLabels.add("Th." + month + "/" + year);
            // Lấy doanh thu & số đơn của tháng đó, nếu chưa có đơn nào thì trả về 0
            chartRevenues.add(revenueByMonth.getOrDefault(key, 0L));
            chartOrderCounts.add(ordersByMonth.getOrDefault(key, 0L));
        }
        
        // Đưa dữ liệu List vào model, Thymeleaf sẽ tự t序列hóa thành JS Array
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartRevenues", chartRevenues);
        model.addAttribute("chartOrderCounts", chartOrderCounts);
        
        return "admin/index";
    }



    @GetMapping("/products")
    public String products(@RequestParam(required = false) Integer categoryId, 
                           @RequestParam(required = false) String keyword, 
                           Model model) {
        model.addAttribute("activeMenu", "products");
        
        List<SanPham> productList;
        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasCategory = (categoryId != null);

        // Logic lọc sản phẩm theo từ khóa (tên) và ID của Hãng Sản Xuất
        if (hasKeyword && hasCategory) {
            productList = sanPhamRepository.findByTenSanPhamContainingIgnoreCaseAndHangSanXuat_Id(keyword.trim(), categoryId);
        } else if (hasKeyword) {
            productList = sanPhamRepository.findByTenSanPhamContainingIgnoreCase(keyword.trim());
        } else if (hasCategory) {
            productList = sanPhamRepository.findByHangSanXuat_Id(categoryId);
        } else {
            productList = sanPhamRepository.findAll();
        }
        
        // Đẩy danh sách sản phẩm và danh sách Hãng (Categories) ra giao diện
        model.addAttribute("products", productList);
        model.addAttribute("categories", hangSanXuatRepository.findAll());
        
        // Lưu lại trạng thái của form tìm kiếm để hiển thị lại trên giao diện
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);
        
        return "admin/product/list";
    }

    /** ─── Mở Form Sửa Sản Phẩm ─── */
    @GetMapping("/products/edit/{id}")
    public String editProductForm(@org.springframework.web.bind.annotation.PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        // Tìm sản phẩm theo ID
        SanPham product = sanPhamRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm có ID: " + id);
            return "redirect:/admin/products";
        }
        
        model.addAttribute("activeMenu", "products");
        model.addAttribute("product", product);
        model.addAttribute("categories", hangSanXuatRepository.findAll());
        model.addAttribute("types", loaiSanPhamRepository.findAll()); // Để chọn Loại (Laptop, Phụ kiện, ...)
        
        return "admin/product/edit";
    }

    /** ─── Xử Lý Nút Lưu (Cập Nhật Sản Phẩm) ─── */
    @PostMapping("/products/update")
    public String updateProduct(@org.springframework.web.bind.annotation.ModelAttribute SanPham formProduct, RedirectAttributes redirectAttributes) {
        // Tìm sản phẩm gốc trong CSDL để đảm bảo không mất các field khác (như danhSachHinhAnh)
        SanPham existingProduct = sanPhamRepository.findById(formProduct.getId()).orElse(null);
        if (existingProduct == null) {
            redirectAttributes.addFlashAttribute("error", "Lưu thất bại: Không tìm thấy sản phẩm.");
            return "redirect:/admin/products";
        }
        
        // Cập nhật các trường thông tin từ form
        existingProduct.setTenSanPham(formProduct.getTenSanPham());
        existingProduct.setGia(formProduct.getGia());
        existingProduct.setSoLuong(formProduct.getSoLuong());
        existingProduct.setMoTaNgan(formProduct.getMoTaNgan());
        existingProduct.setMoTaChiTiet(formProduct.getMoTaChiTiet());
        existingProduct.setHinhAnh(formProduct.getHinhAnh());
        existingProduct.setHangSanXuat(formProduct.getHangSanXuat());
        existingProduct.setLoaiSanPham(formProduct.getLoaiSanPham());
        existingProduct.setNgayCapNhat(LocalDateTime.now());
        
        // Lưu xuống DB
        sanPhamRepository.save(existingProduct);
        redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm #" + existingProduct.getId() + " thành công!");
        
        return "redirect:/admin/products";
    }

    /** ─── Mở Form Thêm Mới Sản Phẩm ─── */
    @GetMapping("/products/create")
    public String createProductForm(Model model) {
        model.addAttribute("activeMenu", "products");
        model.addAttribute("categories", hangSanXuatRepository.findAll());
        model.addAttribute("types", loaiSanPhamRepository.findAll());
        
        return "admin/product/create";
    }

    /** ─── Xử Lý Nút Lưu (Thêm Mới Sản Phẩm) ─── */
    @PostMapping("/products/store")
    public String storeProduct(
            @org.springframework.web.bind.annotation.ModelAttribute SanPham formProduct, 
            @org.springframework.web.bind.annotation.RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        try {
            // Xử lý upload file hình ảnh
            if (!imageFile.isEmpty()) {
                // Lấy tên file gốc
                String fileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
                // Ngừa trùng tên file bằng cách thêm timestamp (tuỳ chọn, ở đây giữ nguyên tên)
                // fileName = System.currentTimeMillis() + "_" + fileName;
                
                // Chỉ định thư mục lưu là src/main/resources/static/img/
                Path uploadDir = Paths.get("src/main/resources/static/img");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                
                Path filePath = uploadDir.resolve(fileName);
                imageFile.transferTo(filePath.toFile());
                
                // Lưu tên file vào database
                formProduct.setHinhAnh(fileName);
            }

            // Set thông tin mặc định cho sản phẩm mới
            formProduct.setNgayTao(LocalDateTime.now());
            formProduct.setNgayCapNhat(LocalDateTime.now());
            formProduct.setDaBan(0);
            formProduct.setTrangThai("Còn hàng");
            
            // Lưu xuống DB
            sanPhamRepository.save(formProduct);
            redirectAttributes.addFlashAttribute("success", "Thêm mới sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi lưu file ảnh: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo sản phẩm: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("activeMenu", "orders");
        
        List<org.example.doan2.entity.DonHang> ordersList;
        if (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("all")) {
            // Nạp tất cả đơn hàng từ mới nhất đến cũ nhất
            ordersList = donHangRepository.findAllByOrderByNgayTaoDesc();
            model.addAttribute("currentStatus", "all");
        } else {
            // Lọc đơn hàng theo trạng thái
            ordersList = donHangRepository.findByTrangThaiOrderByNgayTaoDesc(status);
            model.addAttribute("currentStatus", status);
        }
        
        // Đẩy danh sách ra giao diện `admin/order/list.html`
        model.addAttribute("orders", ordersList);
        return "admin/order/list";
    }

    /** ─── Cập nhật trạng thái một đơn hàng ─── */
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam Integer orderId,
                                    @RequestParam String trangThai,
                                    RedirectAttributes redirectAttributes) {
        // Tìm kiếm đơn hàng theo ID. Nếu tìm thấy, tiến hành đổi trạng thái vào lưu lại CSDL.
        org.example.doan2.entity.DonHang order = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setTrangThai(trangThai);
        order.setNgayCapNhat(LocalDateTime.now());
        donHangRepository.save(order);
        
        // Thêm thông báo "Thành công" và load lại trang danh sách đơn hàng
        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng #" + orderId + " thành công!");
        return "redirect:/admin/orders";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("activeMenu", "users");
        return "admin/user/list";
    }

    @GetMapping("/employees")
    public String employees(Model model) {
        model.addAttribute("activeMenu", "employees");
        return "admin/employee/list";
    }

    /** ─── Hồ sơ cá nhân admin ─── */
    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        String email = authentication.getName();
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("activeMenu", "profile");
        return "admin/profile";
    }

    /** Cập nhật họ tên, SĐT, địa chỉ */
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String hoTen,
                                @RequestParam(required = false) String dienThoai,
                                @RequestParam(required = false) String diaChi,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setHoTen(hoTen);
        user.setDienThoai(dienThoai);
        user.setDiaChi(diaChi);
        user.setNgayCapNhat(LocalDateTime.now());
        nguoiDungRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/admin/profile";
    }

    /** Đổi mật khẩu */
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getMatKhau())) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/admin/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không khớp!");
            return "redirect:/admin/profile";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải ít nhất 6 ký tự!");
            return "redirect:/admin/profile";
        }
        user.setMatKhau(passwordEncoder.encode(newPassword));
        user.setNgayCapNhat(LocalDateTime.now());
        nguoiDungRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        return "redirect:/admin/profile";
    }
}
