package org.example.doan2.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPayService - Dịch vụ tích hợp cổng thanh toán VNPay Sandbox.
 *
 * Hai nhiệm vụ chính:
 *   1. taoUrlThanhToan()  — Xây dựng và trả về URL redirect sang VNPay.
 *   2. xacThucChuKy()     — Kiểm tra chữ ký HMAC-SHA512 khi VNPay callback về.
 */
@Service
public class VNPayService {

    // ── Đọc cấu hình từ application.properties ──────────────────────────────
    @Value("${vnpay.ma-website}")
    private String maWebsite;               // Mã website (TmnCode)

    @Value("${vnpay.chuoi-bi-mat}")
    private String chuoiBiMat;             // Chuỗi bí mật ký HMAC-SHA512

    @Value("${vnpay.duong-dan-cong-thanh-toan}")
    private String duongDanCongThanhToan;  // URL cổng VNPay sandbox

    @Value("${vnpay.url-ket-qua-tra-ve}")
    private String urlKetQuaTraVe;         // URL callback sau thanh toán

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo URL chuyển hướng sang cổng thanh toán VNPay.
     *
     * @param maDonHang   ID đơn hàng (dùng làm mã giao dịch)
     * @param tongTien    Tổng tiền đơn hàng (VNĐ, không có dấu phẩy)
     * @param moTaDonHang Nội dung đơn hàng hiển thị trên VNPay
     * @param yeuCau      HttpServletRequest (để lấy địa chỉ IP khách)
     * @return Đường dẫn URL đầy đủ để redirect sang VNPay
     */
    public String taoUrlThanhToan(int maDonHang, long tongTien, String moTaDonHang, HttpServletRequest yeuCau) {

        // VNPay yêu cầu số tiền nhân 100 (VD: 100,000đ → truyền 10000000)
        long soTienGuiDi = tongTien * 100L;

        // Ngày giờ tạo giao dịch theo định dạng VNPay: yyyyMMddHHmmss
        String thoiGianTaoGiaoDich = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7")).getTime());

        // Mã giao dịch = maDonHang + thời gian (đảm bảo duy nhất)
        String maGiaoDich = maDonHang + "_" + thoiGianTaoGiaoDich;

        // Lấy địa chỉ IP của khách hàng
        String diaChiIpKhach = layDiaChiIp(yeuCau);

        // ── Xây dựng Map tham số gửi lên VNPay (phải sắp xếp theo alphabet) ──
        Map<String, String> mapThamSo = new TreeMap<>();
        mapThamSo.put("vnp_Version",    "2.1.0");
        mapThamSo.put("vnp_Command",    "pay");
        mapThamSo.put("vnp_TmnCode",    maWebsite);
        mapThamSo.put("vnp_Amount",     String.valueOf(soTienGuiDi));
        mapThamSo.put("vnp_CurrCode",   "VND");
        mapThamSo.put("vnp_TxnRef",     maGiaoDich);
        mapThamSo.put("vnp_OrderInfo",  moTaDonHang);
        mapThamSo.put("vnp_OrderType",  "other");
        mapThamSo.put("vnp_Locale",     "vn");
        mapThamSo.put("vnp_ReturnUrl",  urlKetQuaTraVe);
        mapThamSo.put("vnp_IpAddr",     diaChiIpKhach);
        mapThamSo.put("vnp_CreateDate", thoiGianTaoGiaoDich);

        // ── Tạo chuỗi query để ký HMAC-SHA512 ───────────────────────────────
        StringBuilder chuoiQueryKyTen = new StringBuilder();
        StringBuilder chuoiQueryCuoiCung = new StringBuilder();

        for (Map.Entry<String, String> phan : mapThamSo.entrySet()) {
            String giaTriMaHoa = maHoaUrl(phan.getValue());
            if (chuoiQueryKyTen.length() > 0) {
                chuoiQueryKyTen.append("&");
                chuoiQueryCuoiCung.append("&");
            }
            chuoiQueryKyTen.append(phan.getKey()).append("=").append(giaTriMaHoa);
            chuoiQueryCuoiCung.append(phan.getKey()).append("=").append(giaTriMaHoa);
        }

        // ── Tính chữ ký HMAC-SHA512 ─────────────────────────────────────────
        String chuKy = tinhHmacSHA512(chuoiBiMat, chuoiQueryKyTen.toString());
        chuoiQueryCuoiCung.append("&vnp_SecureHash=").append(chuKy);

        // ── Ghép URL hoàn chỉnh ──────────────────────────────────────────────
        return duongDanCongThanhToan + "?" + chuoiQueryCuoiCung;
    }

    /**
     * Xác thực chữ ký HMAC-SHA512 VNPay gửi về trong callback.
     *
     * @param mapThamSoTraVe Toàn bộ query params VNPay gửi về (vnp_xxx=...)
     * @return true nếu chữ ký hợp lệ, false nếu bị giả mạo
     */
    public boolean xacThucChuKy(Map<String, String> mapThamSoTraVe) {
        // Lấy chữ ký VNPay gửi về
        String chuKyNhanDuoc = mapThamSoTraVe.get("vnp_SecureHash");
        if (chuKyNhanDuoc == null) return false;

        // Loại bỏ vnp_SecureHashType và vnp_SecureHash khỏi map trước khi tính lại
        Map<String, String> mapThamSoKhongChuKy = new TreeMap<>(mapThamSoTraVe);
        mapThamSoKhongChuKy.remove("vnp_SecureHash");
        mapThamSoKhongChuKy.remove("vnp_SecureHashType");

        // Xây dựng lại chuỗi query theo đúng định dạng VNPay (replace + thành %20)
        StringBuilder chuoiDuLieu = new StringBuilder();
        for (Map.Entry<String, String> phan : mapThamSoKhongChuKy.entrySet()) {
            if (chuoiDuLieu.length() > 0) chuoiDuLieu.append("&");
            chuoiDuLieu.append(phan.getKey()).append("=").append(maHoaUrl(phan.getValue()));
        }

        // Tính lại chữ ký và so sánh (ignoreCase vì VNPay dùng hex lowercase)
        String chuKyTinhLai = tinhHmacSHA512(chuoiBiMat, chuoiDuLieu.toString());
        return chuKyTinhLai.equalsIgnoreCase(chuKyNhanDuoc);
    }

    /**
     * Mã hóa URL chuẩn theo yêu cầu của VNPay: dùng URLEncoder.
     */
    private String maHoaUrl(String giaTri) {
        try {
            return URLEncoder.encode(giaTri, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return giaTri;
        }
    }

    /**
     * Tính toán chuỗi HMAC-SHA512 từ chuỗi bí mật và dữ liệu đầu vào.
     *
     * @param chuoiBiMat Khóa bí mật (HashSecret)
     * @param duLieu     Chuỗi dữ liệu cần ký
     * @return Chuỗi ký dạng hex
     */
    private String tinhHmacSHA512(String chuoiBiMat, String duLieu) {
        try {
            Mac mayTinh = Mac.getInstance("HmacSHA512");
            SecretKeySpec khoaBiMat = new SecretKeySpec(
                    chuoiBiMat.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mayTinh.init(khoaBiMat);
            byte[] ketQua = mayTinh.doFinal(duLieu.getBytes(StandardCharsets.UTF_8));
            // Chuyển bytes sang chuỗi hex
            StringBuilder chuoiHex = new StringBuilder();
            for (byte b : ketQua) {
                chuoiHex.append(String.format("%02x", b));
            }
            return chuoiHex.toString();
        } catch (Exception loi) {
            throw new RuntimeException("Lỗi khi tính HMAC-SHA512: " + loi.getMessage(), loi);
        }
    }

    /**
     * Lấy địa chỉ IP thực của khách hàng (hỗ trợ cả proxy/load balancer).
     */
    private String layDiaChiIp(HttpServletRequest yeuCau) {
        String diaChiIp = yeuCau.getHeader("X-Forwarded-For");
        if (diaChiIp == null || diaChiIp.isEmpty() || "unknown".equalsIgnoreCase(diaChiIp)) {
            diaChiIp = yeuCau.getHeader("Proxy-Client-IP");
        }
        if (diaChiIp == null || diaChiIp.isEmpty() || "unknown".equalsIgnoreCase(diaChiIp)) {
            diaChiIp = yeuCau.getRemoteAddr();
        }
        // Nếu có nhiều IP (qua nhiều proxy), lấy IP đầu tiên
        if (diaChiIp != null && diaChiIp.contains(",")) {
            diaChiIp = diaChiIp.split(",")[0].trim();
        }
        // Khi chạy localhost, Java trả về IPv6 loopback "0:0:0:0:0:0:0:1" hoặc "::1"
        // VNPay không chấp nhận IPv6 → chuyển về IPv4 127.0.0.1
        if ("0:0:0:0:0:0:0:1".equals(diaChiIp) || "::1".equals(diaChiIp)) {
            diaChiIp = "127.0.0.1";
        }
        return diaChiIp;
    }
}
