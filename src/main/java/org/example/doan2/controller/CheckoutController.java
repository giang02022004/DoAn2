package org.example.doan2.controller;
 
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CheckoutDTO;
import org.example.doan2.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.security.Principal;
 
/**
 * CheckoutController — Xử lý luồng đặt hàng.
 *
 * Phân nhánh theo phương thức thanh toán:
 *   - COD (Thanh toán khi nhận hàng): gọi placeOrder() → thành công → trang order-success
 *   - VNPay: gọi datDonHangChoThanhToan() → lưu đơn "Chờ thanh toán" → redirect sang VNPay
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {
 
    private final OrderService orderService;
 
    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }
 
    @PostMapping("/place-order")
    public String datDonHang(@ModelAttribute CheckoutDTO thongTinDatHang,
                             HttpSession phienLam,
                             HttpServletRequest yeuCau,
                             RedirectAttributes thuocTinhChuyenHuong,
                             Principal nguoiDungHienTai) {
        try {
            if (nguoiDungHienTai == null) {
                return "redirect:/login";
            }
 
            String emailDangNhap = nguoiDungHienTai.getName();
            String phuongThucThanhToan = thongTinDatHang.getPaymentMethod();
 
            // ── Nhánh VNPay: tạo đơn hàng tạm rồi redirect sang cổng VNPay ──
            if ("VNPAY".equals(phuongThucThanhToan)) {
                int maDonHangMoi = orderService.datDonHangChoThanhToan(thongTinDatHang, phienLam, emailDangNhap);
                // Redirect sang VNPayController để tạo URL và chuyển hướng
                return "redirect:/vnpay/tao-thanh-toan?maDonHang=" + maDonHangMoi;
            }
 
            // ── Nhánh COD (mặc định): đặt hàng luôn, không cần VNPay ─────────
            orderService.placeOrder(thongTinDatHang, phienLam, emailDangNhap);
            return "redirect:/checkout/success";
 
        } catch (Exception loi) {
            thuocTinhChuyenHuong.addFlashAttribute("error", "Đặt hàng thất bại: " + loi.getMessage());
            return "redirect:/checkout";
        }
    }
 
    @RequestMapping("/success")
    public String trangDatHangThanhCong() {
        return "order-success";
    }
}
