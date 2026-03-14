package org.example.doan2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = NguoiDung.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private NguoiDung user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(String token, NguoiDung user) {
        this.token = token;
        this.user = user;
        // Token valid for 30 minutes
        this.expiryDate = LocalDateTime.now().plusMinutes(30);
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
