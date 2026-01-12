package org.example.doan2.repository;

import org.example.doan2.entity.HangSanXuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HangSanXuatRepository extends JpaRepository<HangSanXuat, Integer> {
    @Query("""
        SELECT DISTINCT h
        FROM HangSanXuat h
        JOIN SanPham sp ON sp.hangSanXuat.id = h.id
        WHERE sp.loaiSanPham.tenLoai = :tenLoai
    """)
    List<HangSanXuat> findHangByLoaiSanPham(@Param("tenLoai") String tenLoai);
}