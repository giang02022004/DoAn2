package org.example.doan2.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.doan2.entity.DonHang;
import org.example.doan2.repository.DonHangRepository;
import org.example.doan2.service.CartService;
import org.example.doan2.service.VNPayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Map;

/**
 * VNPayController - Xử lý luồng thanh toán VNPay.
 *
 * Luồng hoạt động:
 *   1. GET /vnpay/tao-thanh-toan?maDonHang= → Tạo URL và redirect sang VNPay sandbox
 *   2. GET /vnpay/ket-qua?vnp_...           → VNPay callback, xác thực, cập nhật trạng thái
 */
@Controller
@RequestMapping("/vnpay")
public class VNPayController {

    private final VNPayService vnPayService;
    private final DonHangRepository donHangRepository;
    private final CartService cartService;
    private final org.example.doan2.service.EmailService emailService;
    private final org.example.doan2.service.OrderService orderService;

    public VNPayController(VNPayService vnPayService,
                           DonHangRepository donHangRepository,
                           CartService cartService,
                           org.example.doan2.service.EmailService emailService,
                           org.example.doan2.service.OrderService orderService) {
        this.vnPayService       = vnPayService;
        this.donHangRepository  = donHangRepository;
        this.cartService        = cartService;
        this.emailService       = emailService;
        this.orderService       = orderService;
    }

    /**
     * Bước 1: Tạo URL thanh toán VNPay và chuyển hướng khách hàng.
     * Được gọi từ CheckoutController sau khi đơn hàng đã được lưu ở trạng thái "Chờ thanh toán".
     *
     * @param maDonHang  ID của đơn hàng đã tạo
     * @param yeuCau     HttpServletRequest để lấy IP khách
     * @return Redirect sang trang thanh toán VNPay sandbox
     */
    @GetMapping("/tao-thanh-toan")
    public String taoThanhToan(@RequestParam("maDonHang") int maDonHang,
                                HttpServletRequest yeuCau,
                                Principal nguoiDungHienTai,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        // [BẢO MẬT/LOGIC] Lớp kiểm tra 0: Yêu cầu đăng nhập.
        // Chỉ khách hàng đã đăng nhập mới được gọi API này.
        if (nguoiDungHienTai == null) {
            return "redirect:/login";
        }

        // [BẢO MẬT/LOGIC] Lớp kiểm tra 1: Tồn tại đơn hàng.
        // Chặn trường hợp truyền ID đơn hàng ảo (không có trong DB) lên URL.
        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);
        if (donHang == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/";
        }

        // [BẢO MẬT/LOGIC] Lớp kiểm tra 2: Quyền sở hữu đơn hàng.
        // Ngăn chặn kịch bản User A đổi tham số trên URL để thanh toán/xem trộm đơn hàng của User B.
        if (donHang.getNguoiDung() == null || !donHang.getNguoiDung().getEmail().equals(nguoiDungHienTai.getName())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thanh toán đơn hàng này!");
            return "redirect:/account";
        }

        // [BẢO MẬT/LOGIC] Lớp kiểm tra 3: Trạng thái hợp lệ.
        // Ngăn chặn việc thanh toán lại một đơn hàng đã được thanh toán thành công trước đó,
        // hoặc đơn hàng đã bị quản trị viên / hệ thống huỷ bỏ.
        if ("Đã thanh toán".equals(donHang.getTrangThaiThanhToan()) || "Đã hủy".equals(donHang.getTrangThai())) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng này không hợp lệ để thanh toán!");
            return "redirect:/account";
        }

        // Tạo nội dung thanh toán hiển thị trên cổng VNPay
        String noiDungThanhToan = "Thanh toan don hang #" + maDonHang;

        // Gọi service tạo URL (đã ký HMAC-SHA512)
        String duongDanThanhToan = vnPayService.taoUrlThanhToan(
                maDonHang,
                donHang.getTongTien().longValue(),
                noiDungThanhToan,
                yeuCau
        );

        // Redirect khách sang cổng VNPay
        return "redirect:" + duongDanThanhToan;
    }

    /**
     * Bước 2: Xử lý kết quả VNPay trả về sau khi khách thanh toán.
     * VNPay gọi URL này với các tham số vnp_ResponseCode, vnp_TxnRef, vnp_SecureHash...
     *
     * @param tatCaThamSo Toàn bộ query params VNPay gửi về
     * @param phienLam    Session (để xóa giỏ hàng sau thanh toán thành công)
     * @param model       Model để truyền dữ liệu sang view
     * @return Trang kết quả thanh toán vnpay-ket-qua.html
     */
    @GetMapping("/ket-qua")
    public String xuLyKetQuaThanhToan(HttpServletRequest yeuCau,
                                       HttpSession phienLam,
                                       Principal nguoiDungHienTai,
                                       Model model) {
        
        // --- Bước 0: Quét toàn bộ tham số từ request (cách làm chuẩn của VNPay) ---
        Map<String, String> tatCaThamSo = new java.util.HashMap<>();
        java.util.Enumeration<String> danhSachThamSo = yeuCau.getParameterNames();
        while (danhSachThamSo.hasMoreElements()) {
            String tenThamSo = danhSachThamSo.nextElement();
            String giaTriThamSo = yeuCau.getParameter(tenThamSo);
            if (giaTriThamSo != null && !giaTriThamSo.isEmpty()) {
                tatCaThamSo.put(tenThamSo, giaTriThamSo);
            }
        }

        // ── Bước 2a: Xác thực chữ ký HMAC-SHA512 để chống giả mạo ──────────
        boolean chuKyHopLe = vnPayService.xacThucChuKy(tatCaThamSo);

        // ── Bước 2b: Lấy mã giao dịch (VD: "5_20250306143022") → tách lấy maDonHang ──
        String maGiaoDich  = tatCaThamSo.get("vnp_TxnRef");   // VD: "5_20250306143022"
        String maKetQuaVNP = tatCaThamSo.get("vnp_ResponseCode"); // "00" = thành công

        // Lấy maDonHang từ maGiaoDich (phần trước dấu gạch dưới đầu tiên)
        int maDonHang = Integer.parseInt(maGiaoDich.split("_")[0]);

        // ── Bước 2c: Cập nhật trạng thái đơn hàng ───────────────────────────
        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);

        boolean thanhToanThanhCong = chuKyHopLe && "00".equals(maKetQuaVNP) && donHang != null;

        if (thanhToanThanhCong && donHang != null) {
            // Thanh toán thành công: cập nhật trạng thái và trừ tồn kho
            orderService.xacNhanDonHangVNPayThanhCong(maDonHang, tatCaThamSo.get("vnp_TransactionNo"));

            // Lấy thông tin giỏ hàng trước khi xóa để gửi mail
            String emailDangNhap = (nguoiDungHienTai != null) ? nguoiDungHienTai.getName() : null;
            java.util.List<org.example.doan2.dto.CartItem> cartItems = cartService.getCart(phienLam, emailDangNhap);

            // Gửi email xác nhận
            try {
                emailService.sendOrderConfirmationEmail(donHang.getEmailNhan(), donHang, cartItems);
            } catch (Exception e) {
                System.out.println("Lỗi gửi mail VNPay: " + e.getMessage());
            }

            // Xóa giỏ hàng sau khi thanh toán thành công
            cartService.clearCart(phienLam, emailDangNhap);
        }
        // Nếu thất bại, đơn hàng giữ nguyên "Chờ thanh toán" → admin có thể xem và hủy

        // ── Bước 2d: Truyền dữ liệu sang trang kết quả ──────────────────────
        model.addAttribute("thanhCong",     thanhToanThanhCong);
        model.addAttribute("donHang",       donHang);
        model.addAttribute("maKetQuaVNP",   maKetQuaVNP);
        model.addAttribute("maGiaoDichVNP", tatCaThamSo.get("vnp_TransactionNo"));

        return "vnpay-ket-qua"; // → templates/vnpay-ket-qua.html
    }
}
