package org.example.doan2.service;

import org.example.doan2.entity.SanPham;
import org.example.doan2.repository.SanPhamRepository;
import org.springframework.data.domain.Page;
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

    public List<SanPham> getTop15ByLoaiAndHang(String loai, Integer hangId) {
        Pageable pageable = PageRequest.of(
                0,
                15,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return sanPhamRepository
                .findByLoaiSanPham_TenLoaiAndHangSanXuat_IdOrderByIdDesc(
                        loai, hangId, pageable
                )
                .getContent();
    }

    // TOP 15 theo LOẠI
    public List<SanPham> getTop15ByLoai(String loai) {
        Pageable pageable = PageRequest.of(
                0,
                15,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return sanPhamRepository
                .findByLoaiSanPham_TenLoaiOrderByIdDesc(loai, pageable)
                .getContent();
    }
    //Hết phần hiển thị sản phẩm trên trang chủ


    public Page<SanPham> getPageSanPhams(Pageable pageable) {
        return sanPhamRepository.findAll(pageable);
    }
    
    // Tìm kiếm + Lọc kết hợp (Tên, Hãng, Loại, Giá)
    public Page<SanPham> searchSanPhamsCombined(String keyword, Integer hangId, String loai, Integer maxPrice, Pageable pageable) {
        // Xử lý từ khóa tìm kiếm: nếu rỗng hoặc null thì coi như null (bỏ qua điều kiện like)
        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        
        // Xử lý loại sản phẩm: nếu rỗng thì coi như null
        String searchLoai = (loai != null && !loai.trim().isEmpty()) ? loai.trim() : null;
        
        return sanPhamRepository.findWithFilters(searchKey, hangId, searchLoai, maxPrice, pageable);
    }



    // Lấy danh sách sản phẩm theo giá (nhỏ hơn hoặc bằng maxPrice) và phân trang
    public Page<SanPham> getPageSanPhamsByPrice(Integer maxPrice, Pageable pageable) {
        return sanPhamRepository.findByGiaLessThanEqual(maxPrice, pageable);
    }

    // Lọc theo Hãng
    public Page<SanPham> getSanPhamsByHang(Integer hangId, Pageable pageable) {
        return sanPhamRepository.findByHangSanXuat_Id(hangId, pageable);
    }
    
    // Lấy danh sách sản phẩm theo Hãng và Giá (nhỏ hơn hoặc bằng maxPrice)
    public Page<SanPham> getSanPhamsByHangAndPrice(Integer hangId, Integer maxPrice, Pageable pageable) {
        return sanPhamRepository.findByHangSanXuat_IdAndGiaLessThanEqual(hangId, maxPrice, pageable);
    }

    // Lấy chi tiết sản phẩm
    public SanPham getSanPhamById(Integer id) {
        return sanPhamRepository.findById(id).orElse(null);
    }
    
    // Lọc theo Hãng và Loại
    public Page<SanPham> getSanPhamsByHangAndLoai(Integer hangId, String tenLoai, Pageable pageable) {
        return sanPhamRepository.findByLoaiSanPham_TenLoaiAndHangSanXuat_IdOrderByIdDesc(tenLoai, hangId, pageable);
    }

    // Lấy danh sách sản phẩm theo Hãng, Loại và Giá (nhỏ hơn hoặc bằng maxPrice)
    public Page<SanPham> getSanPhamsByHangAndLoaiAndPrice(Integer hangId, String tenLoai, Integer maxPrice, Pageable pageable) {
        return sanPhamRepository.findByLoaiSanPham_TenLoaiAndHangSanXuat_IdAndGiaLessThanEqualOrderByIdDesc(tenLoai, hangId, maxPrice, pageable);
    }

    // Lấy danh sách sản phẩm liên quan (Top N)
    public List<SanPham> getRelatedProducts(Integer loaiId, Integer excludeId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        return sanPhamRepository.findByLoaiSanPham_IdAndIdNot(loaiId, excludeId, pageable).getContent();
    }
}
