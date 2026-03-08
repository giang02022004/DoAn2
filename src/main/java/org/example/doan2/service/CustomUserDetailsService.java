package org.example.doan2.service;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service tải thông tin người dùng từ database để Spring Security xác thực.
 * Được gọi mỗi khi người dùng đăng nhập.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final NguoiDungRepository nguoiDungRepository;

    public CustomUserDetailsService(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("[AUTH DEBUG] Attempting to login with email: '" + email + "'");
        
        // Tìm người dùng theo email
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("[AUTH DEBUG] User not found with email: '" + email + "'");
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
                
        System.out.println("[AUTH DEBUG] Found user: " + nguoiDung.getEmail() + " | Role: " + nguoiDung.getVaiTro().getTenVaiTro() + " | Status: " + nguoiDung.getTrangThai());

        // Chuyển vai trò thành GrantedAuthority (Spring Security yêu cầu prefix ROLE_)
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().getTenVaiTro())
        );

        // Kiểm tra xem tài khoản có bị khoá không (Mặc định cho tài khoản cũ không có trạng thái là ACTIVE)
        boolean accountNonLocked = nguoiDung.getTrangThai() == null || "ACTIVE".equalsIgnoreCase(nguoiDung.getTrangThai());
        System.out.println("[AUTH DEBUG] Account non-locked status: " + accountNonLocked);

        return new User(
                nguoiDung.getEmail(),
                nguoiDung.getMatKhau(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                accountNonLocked, // accountNonLocked
                authorities
        );
    }
}
