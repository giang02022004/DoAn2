package org.example.doan2.repository;

import org.example.doan2.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Integer> {
    Optional<NguoiDung> findByEmail(String email);
    
    // Lấy danh sách người dùng theo tên vai trò (VD: "USER" hoặc "ADMIN")
    List<NguoiDung> findByVaiTro_TenVaiTro(String tenVaiTro);
    
    // Lấy người dùng thuộc nhiều vai trò khác nhau (VD: ["ADMIN", "EMPLOYEE"])
    List<NguoiDung> findByVaiTro_TenVaiTroIn(List<String> tenVaiTros);
}
