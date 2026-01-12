package org.example.doan2.service;

import org.springframework.stereotype.Service;

import org.example.doan2.entity.HangSanXuat;
import org.example.doan2.repository.HangSanXuatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class HangSanXuatService {
    private final HangSanXuatRepository hangSanXuatRepository;

    public HangSanXuatService(HangSanXuatRepository hangSanXuatRepository) {
        this.hangSanXuatRepository = hangSanXuatRepository;
    }

    /**
     * Lấy danh sách hãng theo loại sản phẩm (LAPTOP / PHU_KIEN)
     */
    public List<HangSanXuat> getHangByLoai(String tenLoai) {
        return hangSanXuatRepository.findHangByLoaiSanPham(tenLoai);
    }
}
