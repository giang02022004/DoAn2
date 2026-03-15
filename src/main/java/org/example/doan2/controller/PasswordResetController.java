package org.example.doan2.controller;

import org.example.doan2.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                       jakarta.servlet.http.HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Dynamic URL generation (Localhost vs Production/Render)
            String siteUrl = request.getScheme() + "://" + request.getServerName();
            if ((request.getScheme().equals("http") && request.getServerPort() != 80) ||
                (request.getScheme().equals("https") && request.getServerPort() != 443)) {
                siteUrl += ":" + request.getServerPort();
            }
            siteUrl += request.getContextPath();

            passwordResetService.createPasswordResetTokenForUser(email, siteUrl);
            redirectAttributes.addFlashAttribute("success", "Vui l\u00F2ng ki\u1EC3m tra email c\u1EE7a b\u1EA1n \u0111\u1EC3 nh\u1EADn li\u00EAn k\u1EBFt kh\u00F4i ph\u1EE5c m\u1EADt kh\u1EA9u.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        String result = passwordResetService.validatePasswordResetToken(token);
        if (result != null) {
            model.addAttribute("error", result.equals("invalidToken") ? "Token kh\u00F4ng h\u1EE3p l\u1EC7." : "Token \u0111\u00E3 h\u1EBFt h\u1EA1n.");
            return "forgot-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.changeUserPassword(token, password);
            redirectAttributes.addFlashAttribute("success", "M\u1EADt kh\u1EA9u c\u1EE7a b\u1EA1n \u0111\u00E3 \u0111\u01B0\u1EE3c thay \u0111\u1EDBi. Vui l\u00F2ng \u0111\u0103ng nh\u1EADp.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}
