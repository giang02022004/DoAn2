package org.example.doan2.repository;

import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(NguoiDung user);
    void deleteByUser(NguoiDung user);
}
