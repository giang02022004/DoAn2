package org.example.doan2.controller;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.VaiTro;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.repository.VaiTroRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
public class AuthController {
    
    private final NguoiDungRepository nguoiDungRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthController(NguoiDungRepository nguoiDungRepository, 
                          VaiTroRepository vaiTroRepository, 
                          PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Hiển thị trang đăng nhập.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    /**
     * Hiển thị form đăng ký tài khoản mới cho khách hàng.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Tạo một đối tượng người dùng trống để liên kết với Form (Thymeleaf)
        model.addAttribute("user", new NguoiDung());
        return "register";
    }
    
    /**
     * Xử lý yêu cầu đăng ký tài khoản từ người dùng.
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") NguoiDung user) {
        // 1. Chuẩn hóa Email: Xóa khoảng trắng và chuyển về chữ thường để tránh lỗi đăng nhập tréo ngoe
        // Ví dụ: "Giang@gmail.com" và "giang@gmail.com" sẽ được hiểu là 1.
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }
        
        System.out.println("[AUTH DEBUG] Đang xử lý đăng ký cho email: " + user.getEmail());

        // 2. Kiểm tra xem Email đã tồn tại trong hệ thống chưa
        if (nguoiDungRepository.findByEmail(user.getEmail()).isPresent()) {
            System.out.println("[AUTH DEBUG] Đăng ký thất bại: Email đã tồn tại: " + user.getEmail());
            return "redirect:/register?error"; // Trả về lỗi nếu email đã có người dùng
        }
        
        // 3. Bảo mật: Mã hóa mật khẩu trước khi lưu vào Database
        user.setMatKhau(passwordEncoder.encode(user.getMatKhau()));
        
        // 4. Thiết lập các giá trị mặc định cho tài khoản mới
        user.setLoaiTaiKhoan("CUSTOMER"); // Loại tài khoản khách hàng
        user.setTrangThai("ACTIVE");     // Tài khoản kích hoạt ngay khi đăng ký
        user.setNgayTao(LocalDateTime.now());
        user.setNgayCapNhat(LocalDateTime.now());
        
        // 5. Phân quyền mặc định (Role): Gán quyền CUSTOMER cho người dùng mới
        // Sử dụng tìm kiếm theo tên, nếu không thấy thì dùng ID dự phòng (thường là 2)
        VaiTro role = vaiTroRepository.findByTenVaiTro("CUSTOMER")
                .or(() -> vaiTroRepository.findById(2))
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy quyền CUSTOMER trong DB"));
        
        user.setVaiTro(role);
        System.out.println("[AUTH DEBUG] Gán quyền: " + role.getTenVaiTro());

        // 6. Lưu người dùng vào Database
        nguoiDungRepository.save(user);
        System.out.println("[AUTH DEBUG] Đăng ký thành công: " + user.getEmail());

        // 7. Chuyển hướng sang trang đăng nhập kèm thông báo thành công
        return "redirect:/login?success";
    }
}
