package org.example.doan2.controller;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
import org.example.doan2.repository.HinhAnhSanPhamRepository;
import org.example.doan2.repository.BienTheSanPhamRepository;
import org.example.doan2.repository.ChiTietDonHangRepository;
import org.example.doan2.repository.VaiTroRepository;
import org.example.doan2.repository.KhuyenMaiRepository;
import org.example.doan2.entity.SanPham;
import org.example.doan2.entity.KhuyenMai;
import org.example.doan2.entity.HangSanXuat;
import org.example.doan2.entity.LoaiSanPham;
import org.example.doan2.entity.HinhAnhSanPham;
import org.example.doan2.entity.BienTheSanPham;
import org.example.doan2.entity.VaiTro;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Tiêm các Repository để lấy dữ liệu thống kê từ database
    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final HangSanXuatRepository hangSanXuatRepository;
    private final LoaiSanPhamRepository loaiSanPhamRepository;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;
    private final KhuyenMaiRepository khuyenMaiRepository;
    private final org.example.doan2.service.OrderService orderService;

    public AdminController(NguoiDungRepository nguoiDungRepository,
                           DonHangRepository donHangRepository,
                           SanPhamRepository sanPhamRepository,
                           HangSanXuatRepository hangSanXuatRepository,
                           LoaiSanPhamRepository loaiSanPhamRepository,
                           HinhAnhSanPhamRepository hinhAnhSanPhamRepository,
                           BienTheSanPhamRepository bienTheSanPhamRepository,
                           ChiTietDonHangRepository chiTietDonHangRepository,
                           VaiTroRepository vaiTroRepository,
                           PasswordEncoder passwordEncoder,
                           KhuyenMaiRepository khuyenMaiRepository,
                           org.example.doan2.service.OrderService orderService) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.donHangRepository = donHangRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.hangSanXuatRepository = hangSanXuatRepository;
        this.loaiSanPhamRepository = loaiSanPhamRepository;
        this.hinhAnhSanPhamRepository = hinhAnhSanPhamRepository;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.passwordEncoder = passwordEncoder;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.orderService = orderService;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model, Authentication authentication) {
        // Redirection for Employee
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            return "redirect:/admin/orders";
        }
        
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
        
        // Đưa dữ liệu List vào model, Thymeleaf sẽ tự động serialize thành JS Array
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartRevenues", chartRevenues);
        model.addAttribute("chartOrderCounts", chartOrderCounts);
        
        // Lấy top 5 sản phẩm bán chạy nhất
        List<Object[]> topProducts = chiTietDonHangRepository.findTopSellingProducts(org.springframework.data.domain.PageRequest.of(0, 5));
        model.addAttribute("topProducts", topProducts);
        
        // Lấy top 5 sản phẩm sắp hết hàng (số lượng thấp nhất)
        List<SanPham> lowStockProducts = sanPhamRepository.findTop5ByOrderBySoLuongAsc();
        model.addAttribute("lowStockProducts", lowStockProducts);
        
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
        
        // Đẩy danh sách sản phẩm (admin xem tất cả, dù đang ẩn hay chưa) và danh sách Hãng ra giao diện
        model.addAttribute("products", productList);
        model.addAttribute("categories", hangSanXuatRepository.findAll());
        
        // Lưu lại trạng thái của form tìm kiếm để hiển thị lại trên giao diện
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategoryId", categoryId);
        
        return "admin/product/list";
    }

    /** ─── Mở Form Sửa Sản Phẩm ─── */
    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
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
        model.addAttribute("promotions", khuyenMaiRepository.findAllAvailable());
        
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
        
        // Lookup managed entities để tránh lỗi "references an unsaved transient instance"
        if (formProduct.getHangSanXuat() != null && formProduct.getHangSanXuat().getId() != null) {
            existingProduct.setHangSanXuat(hangSanXuatRepository.findById(formProduct.getHangSanXuat().getId()).orElse(null));
        }
        
        if (formProduct.getLoaiSanPham() != null && formProduct.getLoaiSanPham().getId() != null) {
            existingProduct.setLoaiSanPham(loaiSanPhamRepository.findById(formProduct.getLoaiSanPham().getId()).orElse(null));
        }

        if (formProduct.getKhuyenMai() != null && formProduct.getKhuyenMai().getId() != null) {
            existingProduct.setKhuyenMai(khuyenMaiRepository.findById(formProduct.getKhuyenMai().getId()).orElse(null));
        } else {
            existingProduct.setKhuyenMai(null);
        }
        
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
        model.addAttribute("promotions", khuyenMaiRepository.findAllAvailable());
        
        return "admin/product/create";
    }

    /** ─── Xử Lý Nút Lưu (Thêm Mới Sản Phẩm) ─── */
    @PostMapping("/products/store")
    public String storeProduct(
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("gia") Integer gia,
            @RequestParam("soLuong") Integer soLuong,
            @RequestParam(value = "moTaNgan", required = false, defaultValue = "") String moTaNgan,
            @RequestParam(value = "moTaChiTiet", required = false, defaultValue = "") String moTaChiTiet,
            @RequestParam("hangSanXuat.id") Integer hangSanXuatId,
            @RequestParam("loaiSanPham.id") Integer loaiSanPhamId,
            @RequestParam(value = "khuyenMai.id", required = false) Integer khuyenMaiId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "cpus", required = false) List<String> cpus,
            @RequestParam(value = "boNhos", required = false) List<String> boNhos,
            @RequestParam(value = "mauSacs", required = false) List<String> mauSacs,
            @RequestParam(value = "giaThems", required = false) List<Integer> giaThems,
            @RequestParam(value = "soLuongBienThes", required = false) List<Integer> soLuongBienThes,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("[DEBUG] storeProduct HTTP POST hit: " + tenSanPham);
            
            // Lookup managed entities từ database để tránh detached entity constraint violation
            HangSanXuat hangSanXuat = hangSanXuatRepository.findById(hangSanXuatId).orElse(null);
            LoaiSanPham loaiSanPham = loaiSanPhamRepository.findById(loaiSanPhamId).orElse(null);
            
            System.out.println("[DEBUG] hxID=" + hangSanXuatId + " -> " + (hangSanXuat != null ? "Found" : "Null"));
            System.out.println("[DEBUG] loaiID=" + loaiSanPhamId + " -> " + (loaiSanPham != null ? "Found" : "Null"));
            
            if (hangSanXuat == null || loaiSanPham == null) {
                System.out.println("[DEBUG] Returning error due to invalid Hang or Loai");
                redirectAttributes.addFlashAttribute("error", "Hãng hoặc Loại sản phẩm không hợp lệ.");
                return "redirect:/admin/products";
            }

            // Build SanPham mới từ các tham số riêng lẻ
            SanPham formProduct = new SanPham();
            formProduct.setTenSanPham(tenSanPham);
            formProduct.setGia(gia);
            formProduct.setSoLuong(soLuong);
            formProduct.setMoTaNgan(moTaNgan);
            formProduct.setMoTaChiTiet(moTaChiTiet);
            formProduct.setHangSanXuat(hangSanXuat);
            formProduct.setLoaiSanPham(loaiSanPham);
            if (khuyenMaiId != null) {
                formProduct.setKhuyenMai(khuyenMaiRepository.findById(khuyenMaiId).orElse(null));
            }
            // Các trường mặc định
            formProduct.setNgayTao(LocalDateTime.now());
            formProduct.setNgayCapNhat(LocalDateTime.now());
            formProduct.setDaBan(0);
            formProduct.setTrangThai("ACTIVE");
            
            // Xử lý upload file hình ảnh (lấy ảnh đầu tiên làm ảnh đại diện chính)
            List<String> savedFileNames = new ArrayList<>();
            if (imageFiles != null && !imageFiles.isEmpty()) {
                Path uploadDir = Paths.get("src/main/resources/static/img").toAbsolutePath();
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                
                // Giới hạn tối đa 5 ảnh
                int limit = Math.min(imageFiles.size(), 5);
                for (int i = 0; i < limit; i++) {
                    MultipartFile file = imageFiles.get(i);
                    if (file != null && !file.isEmpty()) {
                        String fileName = System.currentTimeMillis() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
                        Path filePath = uploadDir.resolve(fileName);
                        file.transferTo(filePath.toFile());
                        savedFileNames.add(fileName);
                    }
                }
                
                // Gắn ảnh đầu tiên làm ảnh đại diện của bảng SanPham
                if (!savedFileNames.isEmpty()) {
                    formProduct.setHinhAnh(savedFileNames.get(0));
                }
            }

            // Tính tổng số lượng từ các biến thể (nếu là Laptop và có nhập cấu hình)
            boolean hasVariants = cpus != null && !cpus.isEmpty();
            if (hasVariants && soLuongBienThes != null) {
                int totalQuantity = 0;
                for (Integer q : soLuongBienThes) {
                    if (q != null) totalQuantity += q;
                }
                // Ghi đè số lượng gốc bằng tổng số lượng biến thể
                formProduct.setSoLuong(totalQuantity);
            }

            // Lưu sản phẩm chính xuống DB trước để lấy ID
            SanPham savedProduct = sanPhamRepository.save(formProduct);

            // Lưu tất cả các ảnh vào bảng HinhAnhSanPham
            for (String fileName : savedFileNames) {
                HinhAnhSanPham hinhAnh = new HinhAnhSanPham();
                hinhAnh.setDuongDan(fileName);
                hinhAnh.setSanPham(savedProduct);
                hinhAnhSanPhamRepository.save(hinhAnh);
            }

            // Lưu các biến thể (Nếu có)
            if (hasVariants) {
                for (int i = 0; i < cpus.size(); i++) {
                    BienTheSanPham bienThe = new BienTheSanPham();
                    bienThe.setCpu(cpus.get(i));
                    bienThe.setBoNho(boNhos != null && i < boNhos.size() ? boNhos.get(i) : "");
                    bienThe.setMauSac(mauSacs != null && i < mauSacs.size() ? mauSacs.get(i) : "");
                    bienThe.setGiaThem(giaThems != null && i < giaThems.size() ? giaThems.get(i) : 0);
                    bienThe.setSoLuong(soLuongBienThes != null && i < soLuongBienThes.size() ? soLuongBienThes.get(i) : 0);
                    bienThe.setDaBan(0);
                    bienThe.setSanPham(savedProduct);
                    
                    bienTheSanPhamRepository.save(bienThe);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Thêm mới sản phẩm thành công!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi lưu file ảnh: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo sản phẩm: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }


    /** ─── Xóa Sản Phẩm ─── */
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // [LOGIC XÓA SẢN PHẨM] Bước 1: Tìm kiếm xem sản phẩm có thực sự tồn tại trong Cơ sở dữ liệu không.
            SanPham product = sanPhamRepository.findById(id).orElse(null);
            
            if (product == null) {
                // Nếu khách truyền ID bậy bạ lên URL -> Báo lỗi không tìm thấy.
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm cần xóa.");
            } else {
                // [LOGIC XÓA MỀM (SOFT DELETE)]
                // Chuyển trạng thái thành "INACTIVE" thay vì xóa hẳn khỏi Database để giữ lại lịch sử đơn hàng.
                product.setTrangThai("INACTIVE");
                sanPhamRepository.save(product);
                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm #" + id + " thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi trong quá trình thao tác: " + e.getMessage());
        }
        
        // Dù thành công hay thất bại, luôn điều hướng admin về lại trang Danh sách sản phẩm.
        return "redirect:/admin/products";
    }

    /** ─── Khôi phục Sản Phẩm (Từ INACTIVE thành ACTIVE) ─── */
    @GetMapping("/products/restore/{id}")
    public String restoreProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Bước 1: Tìm kiếm sản phẩm
            SanPham product = sanPhamRepository.findById(id).orElse(null);
            
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm cần khôi phục.");
            } else {
                // Khôi phục trạng thái về "ACTIVE"
                product.setTrangThai("ACTIVE");
                sanPhamRepository.save(product);
                redirectAttributes.addFlashAttribute("success", "Đã mở bán lại sản phẩm #" + id + " thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi trong quá trình khôi phục: " + e.getMessage());
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

    /** ─── Xem Chi Tiết Đơn Hàng ─── */
    @GetMapping("/orders/detail/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activeMenu", "orders");
        
        // Tìm kiếm đơn hàng
        org.example.doan2.entity.DonHang order = donHangRepository.findById(id).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/admin/orders";
        }
        
        // Lấy danh sách chi tiết đơn hàng (các sản phẩm khách đã đặt)
        List<org.example.doan2.entity.ChiTietDonHang> orderDetails = chiTietDonHangRepository.findByDonHang(order);
        
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);
        
        return "admin/order/detail";
    }

    /** ─── Cập nhật trạng thái một đơn hàng ─── */
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam Integer orderId,
                                    @RequestParam String trangThai,
                                    RedirectAttributes redirectAttributes) {
        orderService.updateStatus(orderId, trangThai);
        
        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng #" + orderId + " thành công!");
        return "redirect:/admin/orders";
    }

    /** ─── Xác nhận thanh toán COD ─── */
    @PostMapping("/orders/confirm-cod")
    public String confirmCodPayment(@RequestParam Integer orderId, RedirectAttributes redirectAttributes) {
        orderService.confirmCodPayment(orderId);
        redirectAttributes.addFlashAttribute("success", "Xác nhận thanh toán cho đơn hàng #" + orderId + " thành công!");
        return "redirect:/admin/orders";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public String customers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("activeMenu", "users");
        // Gọi database tìm người dùng có Role = CUSTOMER (trước đó là USER)
        List<NguoiDung> usersList = nguoiDungRepository.findByVaiTro_TenVaiTro("CUSTOMER");
        
        // Lọc theo từ khoá tìm kiếm (Tên, Email hoặc Số điện thoại)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            usersList = usersList.stream()
                    .filter(u -> (u.getHoTen() != null && u.getHoTen().toLowerCase().contains(kw)) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)) ||
                                 (u.getDienThoai() != null && u.getDienThoai().contains(kw)))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("users", usersList);
        model.addAttribute("keyword", keyword);
        return "admin/user/list";
    }

    /** ─── Khóa / Mở Khóa Tài Khoản ─── */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/toggle-status")
    public String toggleUserStatus(@RequestParam Integer userId, 
                                   @RequestParam String returnUrl,
                                   RedirectAttributes redirectAttributes) {
        NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID: " + userId));
                
        // Toggle trạng thái
        if ("ACTIVE".equalsIgnoreCase(user.getTrangThai())) {
            user.setTrangThai("LOCKED");
            redirectAttributes.addFlashAttribute("success", "Đã khóa tài khoản: " + user.getEmail());
        } else {
            user.setTrangThai("ACTIVE");
            redirectAttributes.addFlashAttribute("success", "Đã mở khóa tài khoản: " + user.getEmail());
        }
        
        user.setNgayCapNhat(LocalDateTime.now());
        nguoiDungRepository.save(user);
        
        // Quay về trang xuất phát (có thể là danh sách user hoặc danh sách employee)
        return "redirect:" + returnUrl;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees")
    public String employees(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("activeMenu", "employees");
        // Giả sử Role Nhân sự quản lý có tên là "ADMIN" và "EMPLOYEE"
        List<String> adminRoles = java.util.Arrays.asList("ADMIN", "EMPLOYEE");
        List<NguoiDung> employeesList = nguoiDungRepository.findByVaiTro_TenVaiTroIn(adminRoles);
        
        // Lọc theo từ khoá tìm kiếm (Tên, Email hoặc Số điện thoại)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            employeesList = employeesList.stream()
                    .filter(u -> (u.getHoTen() != null && u.getHoTen().toLowerCase().contains(kw)) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)) ||
                                 (u.getDienThoai() != null && u.getDienThoai().contains(kw)))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("employees", employeesList);
        model.addAttribute("keyword", keyword);
        return "admin/employee/list";
    }

    /** ─── Thêm Nhân Viên Mới ─── */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees/create")
    public String createEmployee(Model model) {
        model.addAttribute("activeMenu", "employees");
        List<VaiTro> roles = vaiTroRepository.findAll();
        // Lọc bỏ quyền CUSTOMER khỏi danh sách chọn (Admin chỉ phân quyền Quản lý/Nhân viên)
        roles.removeIf(r -> "CUSTOMER".equalsIgnoreCase(r.getTenVaiTro()));
        
        model.addAttribute("roles", roles);
        return "admin/employee/create";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employees/store")
    public String storeEmployee(@RequestParam String email,
                                @RequestParam String matKhau,
                                @RequestParam String hoTen,
                                @RequestParam String dienThoai,
                                @RequestParam Integer vaiTroId,
                                @RequestParam(value = "anhDaiDienFile", required = false) org.springframework.web.multipart.MultipartFile anhDaiDienFile,
                                RedirectAttributes redirectAttributes) {
        
        // Kiểm tra xem Email đã tồn tại chưa
        if (nguoiDungRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email " + email + " đã tồn tại trong hệ thống. Vui lòng chọn Email khác.");
            return "redirect:/admin/employees/create";
        }

        VaiTro role = vaiTroRepository.findById(vaiTroId)
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ!"));

        NguoiDung newEmp = new NguoiDung();
        newEmp.setEmail(email);
        newEmp.setMatKhau(passwordEncoder.encode(matKhau)); // Bắt buộc Hash mật khẩu
        newEmp.setHoTen(hoTen);
        newEmp.setDienThoai(dienThoai);
        newEmp.setVaiTro(role);
        newEmp.setLoaiTaiKhoan(role.getTenVaiTro()); // Đồng bộ loại tài khoản với tên Role
        newEmp.setTrangThai("ACTIVE");
        newEmp.setNgayTao(LocalDateTime.now());
        newEmp.setNgayCapNhat(LocalDateTime.now());
        
        // Xử lý lưu ảnh đại diện
        if (anhDaiDienFile != null && !anhDaiDienFile.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("src/main/resources/static/img");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                String fileName = org.springframework.util.StringUtils.cleanPath(anhDaiDienFile.getOriginalFilename());
                fileName = System.currentTimeMillis() + "_" + fileName; // Tránh trùng tên
                java.nio.file.Path filePath = uploadDir.resolve(fileName).toAbsolutePath();
                System.out.println("[UPLOAD DEBUG] Saving avatar to: " + filePath);
                java.nio.file.Files.copy(anhDaiDienFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                newEmp.setAnhDaiDien(fileName);
                System.out.println("[UPLOAD DEBUG] Avatar saved successfully as: " + fileName);
            } catch (Exception e) {
                System.out.println("[UPLOAD ERROR] Failed to save avatar:");
                e.printStackTrace();
            }
        }
        
        nguoiDungRepository.save(newEmp);

        redirectAttributes.addFlashAttribute("success", "Thêm nhân sự " + hoTen + " thành công!");
        return "redirect:/admin/employees";
    }

    /** ─── Cập Nhật / Phân Quyền Nhân Viên ─── */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees/edit/{id}")
    public String editEmployee(@PathVariable Integer id, Model model) {
        model.addAttribute("activeMenu", "employees");
        
        NguoiDung employee = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhân viên ID: " + id));
        
        List<VaiTro> roles = vaiTroRepository.findAll();
        // Không lọc quyền CUSTOMER ở trang Edit để admin có quyền "giáng chức" một nhân viên nhầm thành Khách hàng.
        
        model.addAttribute("employee", employee);
        model.addAttribute("roles", roles);
        return "admin/employee/edit";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employees/update")
    public String updateEmployee(@RequestParam Integer id,
                                 @RequestParam String hoTen,
                                 @RequestParam String dienThoai,
                                 @RequestParam Integer vaiTroId,
                                 @RequestParam(value = "anhDaiDienFile", required = false) org.springframework.web.multipart.MultipartFile anhDaiDienFile,
                                 RedirectAttributes redirectAttributes) {
                                 
        System.out.println("============= UPLOAD DEBUG =============");
        System.out.println("ID: " + id);
        System.out.println("anhDaiDienFile is null? " + (anhDaiDienFile == null));
        if (anhDaiDienFile != null) {
            System.out.println("anhDaiDienFile empty? " + anhDaiDienFile.isEmpty());
            System.out.println("anhDaiDienFile name: " + anhDaiDienFile.getOriginalFilename());
            System.out.println("anhDaiDienFile size: " + anhDaiDienFile.getSize());
        }
        System.out.println("========================================");
        
        NguoiDung employee = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhân viên ID: " + id));

        VaiTro role = vaiTroRepository.findById(vaiTroId)
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ!"));

        employee.setHoTen(hoTen);
        employee.setDienThoai(dienThoai);
        employee.setVaiTro(role);
        employee.setLoaiTaiKhoan(role.getTenVaiTro());
        
        // Xử lý lưu ảnh đại diện (giữ nguyên ảnh cũ nếu không up hình mới)
        if (anhDaiDienFile != null && !anhDaiDienFile.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("src/main/resources/static/img");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                String fileName = org.springframework.util.StringUtils.cleanPath(anhDaiDienFile.getOriginalFilename());
                fileName = System.currentTimeMillis() + "_" + fileName;
                java.nio.file.Path filePath = uploadDir.resolve(fileName).toAbsolutePath();
                System.out.println("[UPLOAD DEBUG] Updating avatar to: " + filePath);
                java.nio.file.Files.copy(anhDaiDienFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                employee.setAnhDaiDien(fileName);
                System.out.println("[UPLOAD DEBUG] Avatar updated successfully as: " + fileName);
            } catch (Exception e) {
                System.out.println("[UPLOAD ERROR] Failed to update avatar:");
                e.printStackTrace();
            }
        }
        
        employee.setNgayCapNhat(LocalDateTime.now());
        nguoiDungRepository.save(employee);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin phân quyền cho " + hoTen + " thành công!");
        return "redirect:/admin/employees";
    }

    /** ─── Quản Trị Danh Mục (Loại Sản Phẩm) ─── */
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/categories")
    public String listCategories(Model model) {
        // Admin được thấy TOÀN BỘ danh mục (kể cả đã ẩn) để còn phục hồi
        model.addAttribute("categories", loaiSanPhamRepository.findAll());
        model.addAttribute("activeMenu", "categories");
        return "admin/category/list";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/categories/store")
    public String storeCategory(@RequestParam String tenLoai, 
                                @RequestParam(required = false) String moTa,
                                RedirectAttributes redirectAttributes) {
        LoaiSanPham lsp = new LoaiSanPham();
        lsp.setTenLoai(tenLoai);
        lsp.setMoTa(moTa);
        lsp.setTrangThai("ACTIVE");
        loaiSanPhamRepository.save(lsp);
        redirectAttributes.addFlashAttribute("success", "Tạo chuyên mục mới thành công!");
        return "redirect:/admin/categories";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/categories/update")
    public String updateCategory(@RequestParam Integer id, 
                                 @RequestParam String tenLoai, 
                                 @RequestParam(required = false) String moTa,
                                 RedirectAttributes redirectAttributes) {
        LoaiSanPham lsp = loaiSanPhamRepository.findById(id).orElse(null);
        if (lsp != null) {
            lsp.setTenLoai(tenLoai);
            lsp.setMoTa(moTa);
            loaiSanPhamRepository.save(lsp);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hạng mục [" + tenLoai + "] thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục để cập nhật!");
        }
        return "redirect:/admin/categories";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/categories/toggle-status")
    public String toggleCategoryStatus(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        LoaiSanPham lsp = loaiSanPhamRepository.findById(id).orElse(null);
        if (lsp != null) {
            if ("ACTIVE".equals(lsp.getTrangThai())) {
                lsp.setTrangThai("DELETED");
                redirectAttributes.addFlashAttribute("success", "Đã ẨN danh mục: " + lsp.getTenLoai());
            } else {
                lsp.setTrangThai("ACTIVE");
                redirectAttributes.addFlashAttribute("success", "Đã MỞ LẠI danh mục: " + lsp.getTenLoai());
            }
            loaiSanPhamRepository.save(lsp);
        } else {
            redirectAttributes.addFlashAttribute("error", "Lỗi dữ liệu: Không tìm thấy danh mục này!");
        }
        return "redirect:/admin/categories";
    }

    /** ─── Quản Trị Hãng Sản Xuất (HangSanXuat) ─── */
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping("/brands")
    public String listBrands(Model model) {
        // Admin được thấy TOÀN BỘ hãng
        model.addAttribute("brands", hangSanXuatRepository.findAll());
        model.addAttribute("activeMenu", "brands");
        return "admin/brand/list";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/brands/store")
    public String storeBrand(@RequestParam String tenHang, 
                             @RequestParam(required = false) String moTa,
                             RedirectAttributes redirectAttributes) {
        try {
            HangSanXuat h = new HangSanXuat();
            h.setTenHang(tenHang);
            h.setMoTa(moTa != null ? moTa : "");
            h.setTrangThai("ACTIVE");
            hangSanXuatRepository.save(h);
            redirectAttributes.addFlashAttribute("success", "Tạo hãng sản xuất mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Tên Hãng này có thể đã tồn tại trong hệ thống!");
        }
        return "redirect:/admin/brands";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/brands/update")
    public String updateBrand(@RequestParam Integer id, 
                              @RequestParam String tenHang, 
                              @RequestParam(required = false) String moTa,
                              RedirectAttributes redirectAttributes) {
        HangSanXuat h = hangSanXuatRepository.findById(id).orElse(null);
        if (h != null) {
            try {
                h.setTenHang(tenHang);
                h.setMoTa(moTa != null ? moTa : "");
                hangSanXuatRepository.save(h);
                redirectAttributes.addFlashAttribute("success", "Cập nhật hãng [" + tenHang + "] thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Tên Hãng bị trùng lặp!");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy hãng để cập nhật!");
        }
        return "redirect:/admin/brands";
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/brands/toggle-status")
    public String toggleBrandStatus(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        HangSanXuat h = hangSanXuatRepository.findById(id).orElse(null);
        if (h != null) {
            if ("ACTIVE".equals(h.getTrangThai())) {
                h.setTrangThai("DELETED");
                redirectAttributes.addFlashAttribute("success", "Đã ẨN hãng: " + h.getTenHang());
            } else {
                h.setTrangThai("ACTIVE");
                redirectAttributes.addFlashAttribute("success", "Đã MỞ LẠI hãng: " + h.getTenHang());
            }
            hangSanXuatRepository.save(h);
        } else {
            redirectAttributes.addFlashAttribute("error", "Lỗi dữ liệu: Không tìm thấy hãng này!");
        }
        return "redirect:/admin/brands";
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
