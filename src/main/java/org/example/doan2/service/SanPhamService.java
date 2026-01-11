package org.example.doan2.service;

import org.example.doan2.entity.SanPham;
import org.example.doan2.repository.SanPhamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SanPhamService {
    private final SanPhamRepository sanPhamRepository;
    public SanPhamService(SanPhamRepository sanPhamRepository) {
        this.sanPhamRepository = sanPhamRepository;
    }

    public List<SanPham> getTop15MoiNhat() {
        Pageable pageable = PageRequest.of(
                0,
                15,
                Sort.by(Sort.Direction.DESC, "id")
        );
        return sanPhamRepository.findAll(pageable).getContent();
    }

}
