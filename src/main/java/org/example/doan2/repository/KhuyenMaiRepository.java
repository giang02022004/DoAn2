package org.example.doan2.repository;

import org.example.doan2.entity.KhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    
    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai != 'DELETED' OR k.trangThai IS NULL ORDER BY k.id DESC")
    Page<KhuyenMai> findAllActive(Pageable pageable);
    
    @Query("SELECT k FROM KhuyenMai k WHERE k.tenKhuyenMai LIKE %:keyword% AND (k.trangThai != 'DELETED' OR k.trangThai IS NULL) ORDER BY k.id DESC")
    Page<KhuyenMai> searchByTen(String keyword, Pageable pageable);

    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'ACTIVE'")
    List<KhuyenMai> findAllAvailable();
}
