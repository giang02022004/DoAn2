package org.example.doan2.repository;

import org.example.doan2.entity.ChiTietDonHang;
import org.example.doan2.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {
    List<ChiTietDonHang> findByDonHang(DonHang donHang);

    @Query("SELECT c.sanPham, SUM(c.soLuong) as totalSold FROM ChiTietDonHang c WHERE c.donHang.trangThai = 'Đã hoàn thành' GROUP BY c.sanPham ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
}
