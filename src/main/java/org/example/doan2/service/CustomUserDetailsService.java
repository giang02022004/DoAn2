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
        // Tìm người dùng theo email
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Chuyển vai trò thành GrantedAuthority (Spring Security yêu cầu prefix ROLE_)
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().getTenVaiTro())
        );

        return new User(
                nguoiDung.getEmail(),
                nguoiDung.getMatKhau(),
                authorities
        );
    }
}
