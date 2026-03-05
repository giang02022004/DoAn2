package org.example.doan2.service;

import org.example.doan2.entity.DonHang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendPasswordChangeNotification(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("giang220239@student.nctu.edu.vn"); 
        message.setTo(toEmail);
        message.setSubject("Thông báo: Thay đổi mật khẩu thành công");
        message.setText("Chào bạn,\n\nMật khẩu tài khoản của bạn trên hệ thống đã được thay đổi thành công.\n\nNếu bạn không thực hiện việc này, vui lòng liên hệ ngay với ban quản trị để được hỗ trợ.\n\nTrân trọng,\nĐội ngũ Fruitables.");

        try {
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    public void sendOrderConfirmationEmail(String toEmail, DonHang order, java.util.List<org.example.doan2.dto.CartItem> cartItems) {
        try {
            jakarta.mail.internet.MimeMessage message = javaMailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("giang220239@student.nctu.edu.vn"); 
            helper.setTo(toEmail);
            helper.setSubject("Đơn hàng mới: #" + order.getId());
            
            String orderDate = order.getNgayTao() != null ? order.getNgayTao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            
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
            
            for (org.example.doan2.dto.CartItem item : cartItems) {
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
            for (org.example.doan2.dto.CartItem item : cartItems) {
                if (item.getImage() != null && !item.getImage().isEmpty()) {
                    String imgPath = "src/main/resources/static/img/" + item.getImage();
                    java.io.File imgFile = new java.io.File(imgPath);
                    if (imgFile.exists()) {
                        org.springframework.core.io.FileSystemResource res = new org.springframework.core.io.FileSystemResource(imgFile);
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
}
