package org.example.doan2.controller;

import org.example.doan2.entity.ChiTietDonHang;
import org.example.doan2.entity.DonHang;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.ChiTietDonHangRepository;
import org.example.doan2.repository.DonHangRepository;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class AccountController {

    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final org.example.doan2.service.EmailService emailService;
    private final org.example.doan2.service.OrderService orderService;


    public AccountController(NguoiDungRepository nguoiDungRepository,
                             DonHangRepository donHangRepository,
                             ChiTietDonHangRepository chiTietDonHangRepository,
                             org.example.doan2.service.EmailService emailService,
                             org.example.doan2.service.OrderService orderService) {

        this.nguoiDungRepository = nguoiDungRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.emailService = emailService;
        this.orderService = orderService;
    }


    /**
     * Hiển thị trang thông tin tài khoản cá nhân và lịch sử đơn hàng.
     *
     * @param model          Đối tượng để truyền dữ liệu sang View
     * @param authentication Thông tin người dùng hiện tại đang đăng nhập
     * @return Tên View "account" hoặc chuyển hướng sang "/login" nếu chưa đăng nhập
     */
    @GetMapping("/account")
    public String account(Model model, Authentication authentication) {
        // 1. Kiểm tra trạng thái đăng nhập của người dùng
        if (authentication != null && authentication.isAuthenticated()) {
            // 2. Lấy email/username của người dùng hiện tại
            String email = authentication.getName();

            // 3. Tìm kiếm thông tin người dùng chi tiết từ Database
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // 4. Đưa thông tin tài khoản vào Model để hiển thị ở giao diện
                model.addAttribute("user", user);

                // 5. Lấy danh sách đơn hàng của người dùng, sắp xếp giảm dần theo ngày tạo
                List<DonHang> orders = donHangRepository.findByNguoiDungOrderByNgayTaoDesc(user);
                model.addAttribute("orders", orders);
            } else {
                // Nếu session hợp lệ nhưng không tìm thấy user, trả về danh sách đơn hàng trống
                model.addAttribute("orders", Collections.emptyList());
            }
        } else {
            // 6. Nếu chưa đăng nhập hoặc session hết hạn, yêu cầu đăng nhập lại
            return "redirect:/login";
        }

        // 7. Trả về View "account" (account.html)
        return "account";
    }

    /**
     * Cập nhật thông tin cá nhân của người dùng (Họ tên, Số điện thoại, Địa chỉ).
     * Phản hồi dạng JSON để phía Frontend (AJAX) xử lý thông báo.
     */
    @PostMapping("/account/update")
    @ResponseBody
    public Map<String, Object> updateProfile(
            @RequestParam String fullname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Xác định người dùng đang thực hiện yêu cầu
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);

            if (user == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return response;
            }

            // 2. Cập nhật các trường thông tin mới
            user.setHoTen(fullname);
            user.setDienThoai(phone);
            user.setDiaChi(address);
            
            // 3. Cập nhật thời gian sửa đổi cuối cùng
            user.setNgayCapNhat(LocalDateTime.now());

            // 4. Lưu lại vào Database
            nguoiDungRepository.save(user);

            // 5. Phản hồi thành công
            response.put("success", true);
            response.put("message", "Cập nhật thành công!");
        } catch (Exception e) {
            // Trường hợp có lỗi hệ thống hoặc database
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    /**
     * Lấy thông tin chi tiết của một đơn hàng cụ thể.
     * Trả về JSON để hiển thị Popup/Modal chi tiết trên giao diện người dùng.
     */
    @GetMapping("/account/order/{id}")
    @ResponseBody
    public Map<String, Object> getOrderDetail(@PathVariable Integer id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        // Kiểm tra 1: Người dùng phải đăng nhập mới được xem chi tiết đơn hàng
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Chưa đăng nhập");
            return response;
        }

        // Tìm kiếm đơn hàng trong database theo ID
        DonHang order = donHangRepository.findById(id).orElse(null);
        if (order == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng");
            return response;
        }

        // Kiểm tra 2 (Quan trọng): Đảm bảo người dùng chỉ được xem đơn hàng của chính mình
        String currentUserEmail = authentication.getName();
        if (order.getNguoiDung() == null || !currentUserEmail.equalsIgnoreCase(order.getNguoiDung().getEmail())) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền xem đơn hàng này");
            return response;
        }

        // Lấy danh sách các sản phẩm trong đơn hàng (Chi tiết đơn hàng)
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang(order);
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ChiTietDonHang item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", item.getSanPham().getTenSanPham()); // Tên sản phẩm
            m.put("image", item.getSanPham().getHinhAnh());         // Hình ảnh
            m.put("quantity", item.getSoLuong());                  // Số lượng mua
            m.put("price", item.getGia());                         // Giá tại thời điểm mua
            m.put("total", item.getGia() * item.getSoLuong());     // Tổng tiền của item này
            
            // Xử lý thông tin biến thể sản phẩm (CPU, RAM, Màu sắc) nếu có
            if (item.getBienThe() != null) {
                String variantInfo = "";
                if (item.getBienThe().getCpu() != null) variantInfo += item.getBienThe().getCpu();
                if (item.getBienThe().getBoNho() != null) variantInfo += " / " + item.getBienThe().getBoNho();
                if (item.getBienThe().getMauSac() != null) variantInfo += " / " + item.getBienThe().getMauSac();
                m.put("variantInfo", variantInfo);
            }
            itemList.add(m);
        }

        // Tổng hợp thông tin phản hồi JSON
        response.put("success", true);
        response.put("items", itemList);
        response.put("orderId", order.getId());
        response.put("tongTien", order.getTongTien());
        response.put("trangThai", order.getTrangThai());
        response.put("tenNguoiNhan", order.getTenNguoiNhan());
        response.put("diaChiNhan", order.getDiaChiNhan());
        response.put("sdtNhan", order.getDienThoaiNhan());
        
        return response;
    }

    /**
     * Thay đổi mật khẩu tài khoản người dùng.
     * Quy trình bao gồm xác thực mật khẩu cũ, mã hóa mật khẩu mới và gửi email thông báo.
     */
    @PostMapping("/account/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Bước 1: Kiểm tra mật khẩu mới và mật khẩu nhập lại có khớp nhau không
            if (!newPassword.equals(confirmPassword)) {
                response.put("success", false);
                response.put("message", "Mật khẩu mới không khớp!");
                return response;
            }

            // Bước 2: Xác định danh tính người dùng hiện tại
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return response;
            }

            // Bước 3: Sử dụng BCryptPasswordEncoder để kiểm tra mật khẩu cũ
            // Vì mật khẩu trong DB đã bị mã hóa (hashed), ta không thể so sánh trực tiếp
            org.springframework.security.crypto.password.PasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            
            if (!encoder.matches(oldPassword, user.getMatKhau())) {
                response.put("success", false);
                response.put("message", "Mật khẩu hiện tại không đúng!");
                return response;
            }

            // Bước 4: Mã hóa mật khẩu mới và lưu vào cơ sở dữ liệu
            user.setMatKhau(encoder.encode(newPassword));
            user.setNgayCapNhat(LocalDateTime.now());
            nguoiDungRepository.save(user);

            // Bước 5: Gửi email thông báo cho người dùng về việc thay đổi mật khẩu (tăng tính bảo mật)
            emailService.sendPasswordChangeNotification(user.getEmail());

            // Bước 6: Phản hồi thành công cho Client
            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            // Xử lý các lỗi ngoại lệ phát sinh trong quá trình thực hiện
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    /**
     * Xử lý yêu cầu hủy đơn hàng từ phía người dùng.
     * Logic nghiệp vụ chính (kiểm tra trạng thái đơn hàng, hoàn kho...) được thực hiện trong OrderService.
     */
    @PostMapping("/account/order/cancel")
    @ResponseBody
    public Map<String, Object> cancelOrder(@RequestParam Integer orderId, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Bước 1: Kiểm tra quyền truy cập (Người dùng phải đăng nhập)
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Chưa đăng nhập");
                return response;
            }

            // Bước 2: Gọi tầng Service để xử lý logic hủy đơn
            // OrderService sẽ kiểm tra đơn hàng có thuộc về người dùng này không và có đang ở trạng thái cho phép hủy không
            orderService.cancelOrder(orderId, authentication.getName());

            // Bước 3: Phản hồi thành công cho phía Frontend
            response.put("success", true);
            response.put("message", "Hủy đơn hàng thành công!");
        } catch (Exception e) {
            // Bắt các ngoại lệ (ví dụ: đã giao hàng nên không thể hủy, đơn hàng không tồn tại...)
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }
}
