package org.example.doan2.repository;

import org.example.doan2.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn bảng vai_tro.
 * Dùng cho việc gán quyền khi đăng ký tài khoản mới.
 */
@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {

    /**
     * Tìm vai trò theo tên (ví dụ: "ADMIN", "CUSTOMER").
     */
    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);
}
