package org.example.doan2.service;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.PasswordResetToken;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.repository.PasswordResetTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final NguoiDungRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                NguoiDungRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        Optional<NguoiDung> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            NguoiDung user = userOpt.get();
            // Delete old token if exists
            tokenRepository.deleteByUser(user);
            
            String token = UUID.randomUUID().toString();
            PasswordResetToken myToken = new PasswordResetToken(token, user);
            tokenRepository.save(myToken);
            
            emailService.sendPasswordResetEmail(email, token);
        } else {
            throw new RuntimeException("Email kh\u00F4ng t\u1ED3n t\u1EA1i trong h\u1EC7 th\u1ED1ng.");
        }
    }

    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passTokenOpt = tokenRepository.findByToken(token);

        if (passTokenOpt.isEmpty()) {
            return "invalidToken";
        }

        PasswordResetToken passToken = passTokenOpt.get();
        if (passToken.isExpired()) {
            return "expired";
        }

        return null;
    }

    @Transactional
    public void changeUserPassword(String token, String newPassword) {
        Optional<PasswordResetToken> passTokenOpt = tokenRepository.findByToken(token);
        if (passTokenOpt.isPresent()) {
            PasswordResetToken passToken = passTokenOpt.get();
            if (!passToken.isExpired()) {
                NguoiDung user = passToken.getUser();
                user.setMatKhau(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                tokenRepository.delete(passToken);
                
                // Optionally send a notification email
                emailService.sendPasswordChangeNotification(user.getEmail());
            } else {
                throw new RuntimeException("Token \u0111\u00E3 h\u1EBFt h\u1EA1n.");
            }
        } else {
            throw new RuntimeException("Token kh\u00F4ng h\u1EE3p l\u1EC7.");
        }
    }
}
