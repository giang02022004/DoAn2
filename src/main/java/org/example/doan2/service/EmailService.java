package org.example.doan2.service;

import jakarta.mail.internet.MimeMessage;
import org.example.doan2.dto.CartItem;
import org.example.doan2.entity.DonHang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired // Đối tượng dùng để gửi mail qua SMTP
    private JavaMailSender javaMailSender;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private void sendViaResend(String toEmail, String subject, String content, boolean isHtml) {
        log.info("[MAIL DEBUG] Attempting to send email via Resend API to: {}", toEmail);
        
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            throw new RuntimeException("RESEND_API_KEY is missing. Cannot send via API.");
        }

        String url = "https://api.resend.com/emails";
        
        Map<String, Object> body = new HashMap<>();
        body.put("from", "LaptopShop <onboarding@resend.dev>");
        body.put("to", List.of(toEmail));
        body.put("subject", subject);
        
        if (isHtml) {
            body.put("html", content);
        } else {
            body.put("text", content);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + resendApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            log.info("[MAIL DEBUG] Email sent successfully via Resend API!");
        } catch (Exception e) {
            log.error("[MAIL ERROR] Resend API Failure: {}", e.getMessage());
            throw new RuntimeException("Lỗi API Resend: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangeNotification(String toEmail) {
        log.info("[MAIL DEBUG] Sending password change notification to: {}", toEmail);
        String subject = "Thông báo: Thay đổi mật khẩu thành công";
        String content = "Chào bạn,\n\nMật khẩu tài khoản của bạn trên hệ thống đã được thay đổi thành công.\n\nNếu bạn không thực hiện việc này, vui lòng liên hệ ngay với ban quản trị để được hỗ trợ.\n\nTrân trọng,\nĐội ngũ LaptopShop.";

        if (resendApiKey != null && !resendApiKey.isEmpty()) {
            try {
                sendViaResend(toEmail, subject, content, false);
                return;
            } catch (Exception e) {
                log.warn("[MAIL WARN] Resend API failed, falling back to SMTP: {}", e.getMessage());
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("giang220239@student.nctu.edu.vn"); 
        message.setTo(toEmail);
        message.setSubject("Thông báo: Thay đổi mật khẩu thành công");
        message.setText("Chào bạn,\n\nMật khẩu tài khoản của bạn trên hệ thống đã được thay đổi thành công.\n\nNếu bạn không thực hiện việc này, vui lòng liên hệ ngay với ban quản trị để được hỗ trợ.\n\nTrân trọng,\nĐội ngũ LaptopShop.");

        try {
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi gửi email thông báo thay đổi mật khẩu: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, DonHang order, List<CartItem> cartItems) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String senderEmail = "giang220239@student.nctu.edu.vn";
            helper.setFrom(senderEmail); 
            if (toEmail != null) helper.setTo(toEmail);
            helper.setSubject("Đơn hàng mới: #" + order.getId());
            
            String orderDate = order.getNgayTao() != null ? order.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333;\">");
            
            // Header
            html.append("<h3 style=\"color: #8b5cf6; margin-bottom: 5px;\">LaptopShop</h3>");
            html.append("<h1 style=\"margin-top: 0;\">Đơn hàng mới: #").append(order.getId()).append("</h1>");
            html.append("<p>Bạn đã đặt đơn hàng mới từ ").append(order.getTenNguoiNhan()).append(":</p>");
            
            // Tóm tắt đơn hàng
            html.append("<h3 style=\"border-bottom: 2px solid #eee; padding-bottom: 10px; margin-top: 30px;\">Tóm tắt đơn hàng</h3>");
            html.append("<p><a href=\"#\" style=\"color: #8b5cf6; text-decoration: none;\">Đơn hàng #").append(order.getId()).append("</a> (").append(orderDate).append(")</p>");
            
            // Bảng sản phẩm
            html.append("<table style=\"width: 100%; border-collapse: collapse; margin-top: 20px;\">");
            html.append("<thead><tr style=\"border-bottom: 1px solid #eee; text-align: left;\">");
            html.append("<th style=\"padding: 10px 0;\">Sản phẩm</th>");
            html.append("<th style=\"padding: 10px 0; text-align: center;\">Số lượng</th>");
            html.append("<th style=\"padding: 10px 0; text-align: right;\">Giá</th>");
            html.append("</tr></thead><tbody>");
            
            for (CartItem item : cartItems) {
                String imgUrl = "cid:img_" + item.getId();
                String priceStr = String.format("%,d", item.getPrice()).replace(',', '.') + " VND";
                String variantInfo = item.getVariantInfo() != null ? item.getVariantInfo() : "";
                
                html.append("<tr>");
                // Cột Sản phẩm
                html.append("<td style=\"padding: 15px 0; border-bottom: 1px solid #eee;\">");
                html.append("<div style=\"display: flex; align-items: center;\">");
                html.append("<img src=\"").append(imgUrl).append("\" style=\"width: 50px; height: 50px; object-fit: cover; border-radius: 5px; margin-right: 15px;\">");
                html.append("<div><strong>").append(item.getName()).append("</strong>");
                if (!variantInfo.isEmpty()) {
                    html.append("<br><span style=\"color: #666; font-size: 13px;\">").append(variantInfo).append("</span>");
                }
                html.append("</div></div></td>");
                
                // Cột Số lượng
                html.append("<td style=\"padding: 15px 0; border-bottom: 1px solid #eee; text-align: center;\">×").append(item.getQuantity()).append("</td>");
                // Cột Giá
                html.append("<td style=\"padding: 15px 0; border-bottom: 1px solid #eee; text-align: right;\">").append(priceStr).append("</td>");
                html.append("</tr>");
            }
            html.append("</tbody></table>");
            
            // Bảng tổng cộng
            String totalStr = String.format("%,d", order.getTongTien()).replace(',', '.') + " VND";
            html.append("<table style=\"width: 100%; border-collapse: collapse; margin-top: 20px;\">");
            html.append("<tr><td style=\"padding: 5px 0; color: #666;\">Tổng số phụ:</td><td style=\"padding: 5px 0; text-align: right;\">").append(totalStr).append("</td></tr>");
            html.append("<tr><td style=\"padding: 5px 0; color: #666;\">Vận chuyển: Đồng giá</td><td style=\"padding: 5px 0; text-align: right;\">0 VND</td></tr>");
            html.append("<tr><td style=\"padding: 10px 0; font-weight: bold; font-size: 18px;\">Tổng cộng:</td><td style=\"padding: 10px 0; font-weight: bold; font-size: 18px; text-align: right;\">").append(totalStr).append("</td></tr>");
            html.append("<tr><td style=\"padding: 5px 0; color: #666;\">Phương thức thanh toán:</td><td style=\"padding: 5px 0; text-align: right;\">").append(order.getPhuongThucThanhToan()).append("</td></tr>");
            html.append("</table>");
            
            // Địa chỉ
            html.append("<div style=\"margin-top: 40px; display: flex; justify-content: space-between;\">");
            html.append("<div style=\"flex: 1; padding-right: 20px;\">");
            html.append("<h4 style=\"margin-bottom: 10px;\">Địa chỉ thanh toán</h4>");
            html.append("<p style=\"margin: 0; color: #666; line-height: 1.5;\">");
            html.append(order.getTenNguoiNhan()).append("<br>");
            html.append(order.getDiaChiNhan()).append("<br>");
            html.append(order.getDienThoaiNhan()).append("<br>");
            html.append("<a href=\"mailto:").append(order.getEmailNhan() != null ? order.getEmailNhan() : "").append("\" style=\"color: #8b5cf6; text-decoration: none;\">").append(order.getEmailNhan() != null ? order.getEmailNhan() : "").append("</a>");
            html.append("</p></div>");
            
            html.append("<div style=\"flex: 1;\">");
            html.append("<h4 style=\"margin-bottom: 10px;\">Địa chỉ giao hàng</h4>");
            html.append("<p style=\"margin: 0; color: #666; line-height: 1.5;\">");
            html.append(order.getTenNguoiNhan()).append("<br>");
            html.append(order.getDiaChiNhan()).append("<br>");
            html.append(order.getDienThoaiNhan()).append("<br>");
            html.append("</p></div>");
            html.append("</div>");
            
            html.append("<div style=\"text-align: center; margin-top: 40px; color: #666;\">Cảm ơn bạn đã mua sắm tại LaptopShop!</div>");
            html.append("</div>");

            helper.setText(html.toString(), true); 

            // Đính kèm hình ảnh dưới dạng inline
            for (CartItem item : cartItems) {
                if (item.getImage() != null && !item.getImage().isEmpty()) {
                    String imgPath = "src/main/resources/static/img/" + item.getImage();
                    File imgFile = new File(imgPath);
                    if (imgFile.exists() && item.getId() != null) {
                        FileSystemResource res = new FileSystemResource(imgFile);
                        try {
                            helper.addInline("img_" + item.getId(), res);
                        } catch (Exception ex) {
                            System.out.println("Lỗi đính kèm hình ảnh inline: " + ex.getMessage());
                        }
                    }
                }
            }

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi gửi email xác nhận đặt hàng: " + e.getMessage());
        }
    }

    // TEMPORARILY REMOVED @Async to debug synchronous errors on Render
    public void sendPasswordResetEmail(String toEmail, String token, String siteUrl) {
        log.info("[MAIL DEBUG] Preparing reset email for: {}", toEmail);
        
        String resetUrl = siteUrl + "/reset-password?token=" + token;
        String subject = "Yêu cầu khôi phục mật khẩu";
        String content = "Chào bạn,\n\nBạn đã yêu cầu khôi phục mật khẩu cho tài khoản của mình.\n" +
                "Vui lòng nhấp vào đường dẫn dưới đây để đặt lại mật khẩu (liên kết có hiệu lực trong 30 phút):\n\n" +
                resetUrl + "\n\n" +
                "Nếu bạn không yêu cầu thay đổi mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\nĐội ngũ LaptopShop.";

        if (resendApiKey != null && !resendApiKey.isEmpty()) {
            try {
                sendViaResend(toEmail, subject, content, false);
                return;
            } catch (Exception e) {
                log.warn("[MAIL WARN] Resend API failed, falling back to SMTP: {}", e.getMessage());
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("giang220239@student.nctu.edu.vn");
        message.setTo(toEmail);
        message.setSubject(subject);
        
        log.info("[MAIL DEBUG] Reset URL: {}", resetUrl);
        message.setText(content);

        try {
            log.info("[MAIL DEBUG] Sending email via SMTP...");
            javaMailSender.send(message);
            log.info("[MAIL DEBUG] Email sent successfully!");
        } catch (Exception e) {
            log.error("[MAIL ERROR] SMTP Failure: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi máy chủ email: " + e.getMessage());
        }
    }
}
