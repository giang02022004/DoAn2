package org.example.doan2.controller;

import org.example.doan2.entity.DonHang;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.DonHangRepository;
import org.example.doan2.repository.NguoiDungRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;

@Controller
public class AccountController {

    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;

    public AccountController(NguoiDungRepository nguoiDungRepository,
                             DonHangRepository donHangRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.donHangRepository = donHangRepository;
    }

    @GetMapping("/account")
    public String account(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                List<DonHang> orders = donHangRepository.findByNguoiDungOrderByNgayTaoDesc(user);
                model.addAttribute("orders", orders);
            } else {
                model.addAttribute("orders", Collections.emptyList());
            }
        } else {
            return "redirect:/login";
        }
        return "account";
    }
}
