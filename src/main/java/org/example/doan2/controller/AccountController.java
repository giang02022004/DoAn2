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

    public AccountController(NguoiDungRepository nguoiDungRepository,
                             DonHangRepository donHangRepository,
                             ChiTietDonHangRepository chiTietDonHangRepository,
                             org.example.doan2.service.EmailService emailService) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.emailService = emailService;
    }

    @GetMapping("/account")
    public String account(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                List<DonHang> orders = donHangRepository.findByNguoiDungOrderByNgayTaoDesc(user);
                model.addAttribute("orders", orders);
            } else {
                model.addAttribute("orders", Collections.emptyList());
            }
        } else {
            return "redirect:/login";
        }
        return "account";
    }

    @PostMapping("/account/update")
    @ResponseBody
    public Map<String, Object> updateProfile(
            @RequestParam String fullname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return response;
            }
            user.setHoTen(fullname);
            user.setDienThoai(phone);
            user.setDiaChi(address);
            user.setNgayCapNhat(LocalDateTime.now());
            nguoiDungRepository.save(user);
            response.put("success", true);
            response.put("message", "Cập nhật thành công!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/account/order/{id}")
    @ResponseBody
    public Map<String, Object> getOrderDetail(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        DonHang order = donHangRepository.findById(id).orElse(null);
        if (order == null) {
            response.put("success", false);
            return response;
        }
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang(order);
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ChiTietDonHang item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", item.getSanPham().getTenSanPham());
            m.put("image", item.getSanPham().getHinhAnh());
            m.put("quantity", item.getSoLuong());
            m.put("price", item.getGia());
            m.put("total", item.getGia() * item.getSoLuong());
            if (item.getBienThe() != null) {
                String variantInfo = "";
                if (item.getBienThe().getCpu() != null) variantInfo += item.getBienThe().getCpu();
                if (item.getBienThe().getBoNho() != null) variantInfo += " / " + item.getBienThe().getBoNho();
                if (item.getBienThe().getMauSac() != null) variantInfo += " / " + item.getBienThe().getMauSac();
                m.put("variantInfo", variantInfo);
            }
            itemList.add(m);
        }
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

    @PostMapping("/account/change-password")
    @ResponseBody
    public Map<String, Object> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!newPassword.equals(confirmPassword)) {
                response.put("success", false);
                response.put("message", "Mật khẩu mới không khớp!");
                return response;
            }
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản");
                return response;
            }
            org.springframework.security.crypto.password.PasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            if (!encoder.matches(oldPassword, user.getMatKhau())) {
                response.put("success", false);
                response.put("message", "Mật khẩu hiện tại không đúng!");
                return response;
            }
            user.setMatKhau(encoder.encode(newPassword));
            user.setNgayCapNhat(LocalDateTime.now());
            nguoiDungRepository.save(user);

            // Gửi email thông báo
            emailService.sendPasswordChangeNotification(user.getEmail());

            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        return response;
    }
}
