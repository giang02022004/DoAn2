package org.example.doan2.repository;

import org.example.doan2.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    Page<SanPham> findByLoaiSanPham_TenLoaiOrderByIdDesc(
            String tenLoai,
            Pageable pageable
    );
}