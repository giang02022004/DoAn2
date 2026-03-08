package org.example.doan2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    public SecurityConfig(CustomLoginSuccessHandler customLoginSuccessHandler) {
        this.customLoginSuccessHandler = customLoginSuccessHandler;
    }


    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.sendRedirect(request.getContextPath() + "/");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) ->
                        authorize
                                // 1. CỔNG VÀO KHU VỰC QUẢN TRỊ (/admin/**)
                                // Cho phép cả ADMIN (Quản trị cao nhất) và EMPLOYEE (Nhân sự vận hành) được đi qua cánh cổng chung này.
                                // Tính bảo mật phân lớp: Mặc dù EMPLOYEE qua được cổng này, nhưng khi truy cập vào từng chức năng cụ thể
                                // (ví dụ: quản lý nhân sự, xem doanh thu), hệ thống sẽ kiểm tra thêm thẻ @PreAuthorize trên từng Controller để bắt giữ lại nếu vi phạm.
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "EMPLOYEE")
                                
                                // Khu vực Thanh toán (/checkout, /vnpay) và Tài khoản khách hàng (/account)
                                // bây giờ được siết chặt: CHỈ Khách Hàng mới được phép vào. Admin đứng ngoài.
                                .requestMatchers("/checkout/**", "/account/**", "/vnpay/**").hasRole("CUSTOMER")
                                
                                // Những trang còn lại (ngắm sản phẩm, trang chủ...) thì mở cửa tự do
                                .anyRequest().permitAll()
                )
                .formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .successHandler(customLoginSuccessHandler)  // Redirect thông minh theo role
                                .permitAll()
                )
                .logout(
                        logout -> logout
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/login?logout")
                                .logoutRequestMatcher(request -> request.getServletPath().equals("/logout"))
                                .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler())
                );
        return http.build();
    }
}
