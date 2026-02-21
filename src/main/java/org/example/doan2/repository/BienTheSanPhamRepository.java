package org.example.doan2.repository;

import org.example.doan2.entity.BienTheSanPham;
import org.example.doan2.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BienTheSanPhamRepository extends JpaRepository<BienTheSanPham, Integer> {
    List<BienTheSanPham> findBySanPham(SanPham sanPham);
}
