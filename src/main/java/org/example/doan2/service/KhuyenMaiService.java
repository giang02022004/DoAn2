package org.example.doan2.service;

import org.example.doan2.entity.KhuyenMai;
import org.example.doan2.repository.KhuyenMaiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;

    public Page<KhuyenMai> getKhuyenMais(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return khuyenMaiRepository.searchByTen(keyword, pageable);
        }
        return khuyenMaiRepository.findAllActive(pageable);
    }

    public List<KhuyenMai> getAllAvailable() {
        return khuyenMaiRepository.findAllAvailable();
    }

    public KhuyenMai saveKhuyenMai(KhuyenMai khuyenMai) {
        if (khuyenMai.getTrangThai() == null || khuyenMai.getTrangThai().isEmpty()) {
            khuyenMai.setTrangThai("ACTIVE");
        }
        return khuyenMaiRepository.save(khuyenMai);
    }

    public KhuyenMai getKhuyenMaiById(Integer id) {
        Optional<KhuyenMai> optional = khuyenMaiRepository.findById(id);
        return optional.orElse(null);
    }

    public void deleteKhuyenMai(Integer id) {
        KhuyenMai km = getKhuyenMaiById(id);
        if (km != null) {
            km.setTrangThai("DELETED"); // Soft delete
            khuyenMaiRepository.save(km);
        }
    }
}
