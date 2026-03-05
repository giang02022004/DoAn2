package org.example.doan2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình quy tắc MVC riêng của Web.
 * Nhiệm vụ hiện tại: Đăng ký cái anh bảo vệ AdminRedirectInterceptor đứng chặn ở cổng vào HTTP
 * để anh ấy thực hiện nhiệm vụ lọc request.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminRedirectInterceptor adminRedirectInterceptor;

    // Nhúng (Inject) đối tượng bảo vệ đã được Spring tạo vào đây
    public WebConfig(AdminRedirectInterceptor adminRedirectInterceptor) {
        this.adminRedirectInterceptor = adminRedirectInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Đăng ký "anh bảo vệ" này và dặn anh ấy rà soát TOÀN BỘ tất cả các ngõ ngách (/**) trong hệ thống
        registry.addInterceptor(adminRedirectInterceptor).addPathPatterns("/**");
    }
}
