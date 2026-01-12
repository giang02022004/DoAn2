package org.example.doan2.repository;

import org.example.doan2.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;



public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    // LẤY TOP 15 THEO LOẠI
    Page<SanPham> findByLoaiSanPham_TenLoaiOrderByIdDesc(
            String tenLoai,
            Pageable pageable
    );

    // LẤY TOP 15 THEO LOẠI + HÃNG
    Page<SanPham> findByLoaiSanPham_TenLoaiAndHangSanXuat_IdOrderByIdDesc(
            String tenLoai,
            Integer hangId,
            Pageable pageable
    );
}