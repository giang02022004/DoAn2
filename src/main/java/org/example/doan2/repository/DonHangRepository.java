package org.example.doan2.repository;

import org.example.doan2.entity.DonHang;
import org.example.doan2.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {
    List<DonHang> findByNguoiDungOrderByNgayTaoDesc(NguoiDung nguoiDung);
}
