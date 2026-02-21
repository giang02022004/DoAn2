package org.example.doan2.repository;

import org.example.doan2.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    // LẤY TOP 15 THEO LOẠI
    Page<SanPham> findByLoaiSanPham_TenLoaiOrderByIdDesc(
            String tenLoai,
            Pageable pageable
    );

    // LẤY TOP 15 THEO LOẠI + HÃNG
    Page<SanPham> findByLoaiSanPham_TenLoaiAndHangSanXuat_IdOrderByIdDesc(
            String tenLoai,
            Integer hangId,
            Pageable pageable
    );

    // LẤY TẤT CẢ THEO HÃNG (Phân trang)
    Page<SanPham> findByHangSanXuat_Id(Integer hangId, Pageable pageable);

    // LỌC THEO GIÁ (Max Price)
    Page<SanPham> findByGiaLessThanEqual(Integer maxPrice, Pageable pageable);

    // LỌC THEO HÃNG + GIÁ
    Page<SanPham> findByHangSanXuat_IdAndGiaLessThanEqual(Integer hangId, Integer maxPrice, Pageable pageable);

    // LỌC THEO LOẠI + HÃNG + GIÁ
    Page<SanPham> findByLoaiSanPham_TenLoaiAndHangSanXuat_IdAndGiaLessThanEqualOrderByIdDesc(
            String tenLoai,
            Integer hangId,
            Integer maxPrice,
            Pageable pageable
    );

    // LẤY SẢN PHẨM LIÊN QUAN (Theo loại, trừ sản phẩm hiện tại)
    Page<SanPham> findByLoaiSanPham_IdAndIdNot(Integer loaiId, Integer excludeId, Pageable pageable);

    // TÌM KIẾM KẾT HỢP (Từ khóa + Hãng + Loại + Giá)
    // Nếu tham số truyền vào là NULL, điều kiện đó sẽ bị bỏ qua (luôn đúng)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM SanPham s WHERE " +
            "(:keyword IS NULL OR LOWER(s.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:hangId IS NULL OR s.hangSanXuat.id = :hangId) AND " +
            "(:loai IS NULL OR s.loaiSanPham.tenLoai = :loai) AND " +
            "(:maxPrice IS NULL OR s.gia <= :maxPrice)")
    Page<SanPham> findWithFilters(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("hangId") Integer hangId,
            @org.springframework.data.repository.query.Param("loai") String loai,
            @org.springframework.data.repository.query.Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );
}