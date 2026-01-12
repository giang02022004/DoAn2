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

    public List<SanPham> getLaptop() {
        Pageable pageable = PageRequest.of(
                0,
                15,
                Sort.by(Sort.Direction.DESC, "id")
        );
        return this.sanPhamRepository.findByLoaiSanPham_TenLoaiOrderByIdDesc("LAPTOP", pageable).getContent();
    }
    public List<SanPham> getPhuKien() {
        Pageable pageable = PageRequest.of(
                0,
                15,
                Sort.by(Sort.Direction.DESC, "id")
        );
        return this.sanPhamRepository.findByLoaiSanPham_TenLoaiOrderByIdDesc("PHU_KIEN", pageable).getContent();
    }
    //Hết phần hiển thị sản phẩm trên trang chủ


}
