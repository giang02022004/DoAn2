package org.example.doan2.config;

import org.example.doan2.entity.BaiViet;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.BaiVietRepository;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class ArticleDataInitializer implements CommandLineRunner {

    private final BaiVietRepository baiVietRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public ArticleDataInitializer(BaiVietRepository baiVietRepository, NguoiDungRepository nguoiDungRepository) {
        this.baiVietRepository = baiVietRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (baiVietRepository.count() == 0) {
            // Lấy một Admin bất kỳ làm tác giả
            NguoiDung admin = nguoiDungRepository.findAll().stream()
                    .filter(u -> u.getVaiTro() != null && u.getVaiTro().getTenVaiTro().contains("ADMIN"))
                    .findFirst()
                    .orElse(null);

            if (admin == null) {
                // Nếu không có admin nào, lấy người dùng đầu tiên
                admin = nguoiDungRepository.findAll().stream().findFirst().orElse(null);
            }

            if (admin != null) {
                BaiViet sample = new BaiViet();
                sample.setTieuDe("Top 5 Laptop Đáng Mua Nhất Cho Sinh Viên Năm 2024");
                sample.setNoiDungNgan("Khám phá danh sách các mẫu laptop có hiệu năng tốt, giá cả phải chăng phù hợp cho nhu cầu học tập và giải trí của sinh viên.");
                
                String richContent = "<div style='font-family: inherit; line-height: 1.8;'>" +
                        "<p>Trong năm 2024, thị trường laptop đang trở nên sôi động hơn bao giờ hết với sự xuất hiện của các dòng chip mới mạnh mẽ và tiết kiệm điện. Đối với sinh viên, một chiếc laptop không chỉ là công cụ học tập mà còn là thiết bị giải trí không thể thiếu.</p>" +
                        "<h3 style='color: #81c408; border-left: 5px solid #81c408; padding-left: 15px; margin: 25px 0 15px;'>1. MacBook Air M2 - Sang Trọng và Bền Bỉ</h3>" +
                        "<p>Dù đã ra mắt được một thời gian, MacBook Air M2 vẫn là sự lựa chọn tối ưu nhờ trọng lượng nhẹ, thời lượng pin ấn tượng lên đến 15-18 tiếng, rất phù hợp cho việc mang đến giảng đường.</p>" +
                        "<ul style='list-style-type: square; padding-left: 20px;'>" +
                        "<li><strong>Ưu điểm:</strong> Màn hình Liquid Retina cực đẹp, không cần quạt tản nhiệt nên cực kỳ yên tĩnh.</li>" +
                        "<li><strong>Phù hợp cho:</strong> Sinh viên kinh tế, ngôn ngữ, marketing.</li>" +
                        "</ul>" +
                        "<h3 style='color: #81c408; border-left: 5px solid #81c408; padding-left: 15px; margin: 25px 0 15px;'>2. ASUS Vivobook 15 OLED - Màn Hình Tuyệt Đỉnh</h3>" +
                        "<p>Nếu bạn là người yêu thích điện ảnh hoặc thường xuyên thiết kế slide bài giảng, màn hình OLED trên Vivobook sẽ không làm bạn thất vọng.</p>" +
                        "<blockquote style='background: #f8f9fa; border-radius: 8px; padding: 15px; margin: 20px 0; font-style: italic; color: #555;'>" +
                        "&ldquo;Một chiếc laptop với màn hình OLED trong tầm giá sinh viên là một món hời lớn từ ASUS.&rdquo;" +
                        "</blockquote>" +
                        "<h3 style='color: #81c408; border-left: 5px solid #81c408; padding-left: 15px; margin: 25px 0 15px;'>Kết Luận</h3>" +
                        "<p>Tùy thuộc vào ngân sách và ngành học mà bạn có thể chọn cho mình chiếc máy ưng ý nhất. Đừng quên ghé qua <strong>LaptopShop</strong> để nhận ưu đãi mùa tựu trường nhé!</p>" +
                        "</div>";
                
                sample.setNoiDungChiTiet(richContent);
                sample.setTrangThai("ACTIVE");
                sample.setNgayTao(LocalDateTime.now());
                sample.setNgayCapNhat(LocalDateTime.now());
                sample.setTacGia(admin);
                sample.setHinhAnh("articles/laptop-sample.jpg");

                baiVietRepository.save(sample);
                System.out.println(">>> Đã tạo bài viết mẫu thành công!");
            }
        }
    }
}
