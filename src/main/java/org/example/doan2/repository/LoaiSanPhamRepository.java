package org.example.doan2.repository;

import org.example.doan2.entity.LoaiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoaiSanPhamRepository extends JpaRepository<LoaiSanPham, Integer> {
    
    // Lấy tất cả danh mục chưa bị xoá mềm
    @org.springframework.data.jpa.repository.Query("SELECT l FROM LoaiSanPham l WHERE l.trangThai IS NULL OR l.trangThai = 'ACTIVE'")
    java.util.List<LoaiSanPham> findAllActive();
}
