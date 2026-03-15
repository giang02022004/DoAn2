package org.example.doan2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.BadCredentialsException;

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
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            String errorMessage = "Email hoặc mật khẩu không chính xác.";
            if (exception instanceof LockedException) {
                errorMessage = "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.";
            } else if (exception instanceof BadCredentialsException) {
                errorMessage = "Email hoặc mật khẩu không chính xác.";
            }
            
            System.out.println("[AUTH DEBUG] Login failed: " + exception.getMessage());
            request.getSession().setAttribute("loginError", errorMessage);
            response.sendRedirect(request.getContextPath() + "/login?error");
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
                                
                                // Cho phép VNPay gọi IPN và các luồng khôi phục mật khẩu (không cần đăng nhập)
                                .requestMatchers("/vnpay/vnpay-ipn", "/forgot-password", "/reset-password").permitAll()
                                
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
                                .failureHandler(customAuthenticationFailureHandler()) // Xử lý lỗi chi tiết
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
