package org.example.doan2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
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
                                // Khu vực /admin/** chỉ cấp phép duy nhất cho tài khoản mang quyền ADMIN
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                
                                // Khu vực Thanh toán (/checkout) và Tài khoản khách hàng (/account) 
                                // bây giờ được siết chặt: CHỈ Khách Hàng mới được phép vào. Admin đứng ngoài.
                                .requestMatchers("/checkout/**", "/account/**").hasRole("CUSTOMER")
                                
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
