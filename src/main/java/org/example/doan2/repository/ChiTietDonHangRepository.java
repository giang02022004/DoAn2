package org.example.doan2.repository;

import org.example.doan2.entity.ChiTietDonHang;
import org.example.doan2.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {
    List<ChiTietDonHang> findByDonHang(DonHang donHang);
}
