package org.example.doan2.controller;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.VaiTro;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.repository.VaiTroRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
public class AuthController {
    
    private final NguoiDungRepository nguoiDungRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthController(NguoiDungRepository nguoiDungRepository, 
                          VaiTroRepository vaiTroRepository, 
                          PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new NguoiDung());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") NguoiDung user) {
        if (nguoiDungRepository.findByEmail(user.getEmail()).isPresent()) {
            return "redirect:/register?error";
        }
        
        user.setMatKhau(passwordEncoder.encode(user.getMatKhau()));
        user.setLoaiTaiKhoan("CUSTOMER");
        user.setNgayTao(LocalDateTime.now());
        user.setNgayCapNhat(LocalDateTime.now());
        
        VaiTro role = vaiTroRepository.findByTenVaiTro("CUSTOMER")
                .or(() -> vaiTroRepository.findById(1))
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setVaiTro(role);

        nguoiDungRepository.save(user);

        return "redirect:/login?success";
    }
}
