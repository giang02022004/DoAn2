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
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/vnpay")
public class VNPayController {

    private static final String STATUS_DA_THANH_TOAN = "\u0110\u00E3 thanh to\u00E1n";
    private static final String STATUS_DA_HUY = "\u0110\u00E3 h\u1EE7y";

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
        this.vnPayService = vnPayService;
        this.donHangRepository = donHangRepository;
        this.cartService = cartService;
        this.emailService = emailService;
        this.orderService = orderService;
    }

    @GetMapping("/tao-thanh-toan")
    public String taoThanhToan(@RequestParam("maDonHang") int maDonHang,
                               HttpServletRequest yeuCau,
                               Principal nguoiDungHienTai,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (nguoiDungHienTai == null) {
            return "redirect:/login";
        }

        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);
        if (donHang == null) {
            redirectAttributes.addFlashAttribute("error", "Kh\u00F4ng t\u00ECm th\u1EA5y \u0111\u01A1n h\u00E0ng!");
            return "redirect:/";
        }

        if (donHang.getNguoiDung() == null || !donHang.getNguoiDung().getEmail().equals(nguoiDungHienTai.getName())) {
            redirectAttributes.addFlashAttribute("error", "B\u1EA1n kh\u00F4ng c\u00F3 quy\u1EC1n thanh to\u00E1n \u0111\u01A1n h\u00E0ng n\u00E0y!");
            return "redirect:/account";
        }

        if (STATUS_DA_THANH_TOAN.equals(donHang.getTrangThaiThanhToan()) || STATUS_DA_HUY.equals(donHang.getTrangThai())) {
            redirectAttributes.addFlashAttribute("error", "\u0110\u01A1n h\u00E0ng n\u00E0y kh\u00F4ng h\u1EE3p l\u1EC7 \u0111\u1EC3 thanh to\u00E1n!");
            return "redirect:/account";
        }

        String noiDungThanhToan = "Thanh toan don hang #" + maDonHang;
        String duongDanThanhToan = vnPayService.taoUrlThanhToan(
                maDonHang,
                donHang.getTongTien().longValue(),
                noiDungThanhToan,
                yeuCau
        );

        return "redirect:" + duongDanThanhToan;
    }

    @GetMapping("/ket-qua")
    public String xuLyKetQuaThanhToan(HttpServletRequest yeuCau,
                                      HttpSession phienLam,
                                      Principal nguoiDungHienTai,
                                      Model model) {

        Map<String, String> tatCaThamSo = vnpParamsMap(yeuCau);

        boolean chuKyHopLe = vnPayService.xacThucChuKy(tatCaThamSo);
        String maGiaoDich = tatCaThamSo.get("vnp_TxnRef");
        String maKetQuaVNP = tatCaThamSo.get("vnp_ResponseCode");

        if (maGiaoDich == null || !maGiaoDich.contains("_")) {
            model.addAttribute("thanhCong", false);
            model.addAttribute("error", "Tham số giao dịch không hợp lệ.");
            return "vnpay-ket-qua";
        }

        int maDonHang = Integer.parseInt(maGiaoDich.split("_")[0]);
        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);

        boolean thanhToanThanhCong = chuKyHopLe && "00".equals(maKetQuaVNP) && donHang != null;

        if (thanhToanThanhCong) {
            // Xác nhận đơn hàng (Idempotent)
            orderService.xacNhanDonHangVNPayThanhCong(maDonHang, tatCaThamSo.get("vnp_TransactionNo"));
            
            // Xóa giỏ hàng trong Session (vì khách đang thao tác trên trình duyệt)
            String emailDangNhap = (nguoiDungHienTai != null) ? nguoiDungHienTai.getName() : null;
            cartService.clearCart(phienLam, emailDangNhap);
        }

        model.addAttribute("thanhCong", thanhToanThanhCong);
        model.addAttribute("donHang", donHang);
        model.addAttribute("maKetQuaVNP", maKetQuaVNP);
        model.addAttribute("maGiaoDichVNP", tatCaThamSo.get("vnp_TransactionNo"));

        return "vnpay-ket-qua";
    }

    /**
     * IPN Endpoint: VNPay Server gọi ngầm đến đây để xác nhận trạng thái thanh toán.
     * Đảm bảo đơn hàng được xác nhận kể cả khi khách đóng trình duyệt.
     */
    @GetMapping("/vnpay-ipn")
    @ResponseBody
    public Map<String, String> vnpayIPN(HttpServletRequest request) {
        Map<String, String> params = vnpParamsMap(request);
        Map<String, String> response = new HashMap<>();

        try {
            if (vnPayService.xacThucChuKy(params)) {
                String txnRef = params.get("vnp_TxnRef");
                if (txnRef == null || !txnRef.contains("_")) {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                    return response;
                }
                int orderId = Integer.parseInt(txnRef.split("_")[0]);
                String vnpResponseCode = params.get("vnp_ResponseCode");

                if ("00".equals(vnpResponseCode)) {
                    // Xử lý xác nhận đơn hàng thành công
                    boolean isFirstSuccess = orderService.xacNhanDonHangVNPayThanhCong(orderId, params.get("vnp_TransactionNo"));
                    
                    if (isFirstSuccess) {
                        // Gửi email xác nhận (IPN là nơi uy tín nhất để gửi)
                        DonHang donHang = donHangRepository.findById(orderId).orElse(null);
                        if (donHang != null) {
                            try {
                                emailService.sendOrderConfirmationEmail(donHang.getEmailNhan(), donHang, java.util.Collections.emptyList());
                            } catch (Exception e) {
                                System.out.println("Loi gui mail IPN: " + e.getMessage());
                            }
                        }
                    }
                    
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");
                } else {
                    response.put("RspCode", "00");
                    response.put("Message", "Payment Failed recorded");
                }
            } else {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            response.put("RspCode", "99");
            response.put("Message", "Unknown Error");
        }
        return response;
    }

    private Map<String, String> vnpParamsMap(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        java.util.Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                vnpParams.put(paramName, paramValue);
            }
        }
        return vnpParams;
    }
}
