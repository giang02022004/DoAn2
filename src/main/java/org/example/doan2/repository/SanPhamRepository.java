package org.example.doan2.repository;

import org.example.doan2.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    // LẤY TOP 15 SẢN PHẨM ĐANG HOẠT ĐỘNG THEO LOẠI (Dùng cho Trang chủ)
    Page<SanPham> findByTrangThaiAndLoaiSanPham_TenLoaiOrderByIdDesc(
            String trangThai,
            String tenLoai,
            Pageable pageable
    );

    // LẤY TOP 15 SẢN PHẨM ĐANG HOẠT ĐỘNG THEO LOẠI + HÃNG (Dùng cho Trang chủ khi lọc)
    Page<SanPham> findByTrangThaiAndLoaiSanPham_TenLoaiAndHangSanXuat_IdOrderByIdDesc(
            String trangThai,
            String tenLoai,
            Integer hangId,
            Pageable pageable
    );

    // LẤY TẤT CẢ SẢN PHẨM ĐANG HOẠT ĐỘNG THEO HÃNG (Phân trang)
    Page<SanPham> findByTrangThaiAndHangSanXuat_Id(String trangThai, Integer hangId, Pageable pageable);

    // LỌC SẢN PHẨM ĐANG HOẠT ĐỘNG THEO GIÁ (Max Price)
    Page<SanPham> findByTrangThaiAndGiaLessThanEqual(String trangThai, Integer maxPrice, Pageable pageable);

    // LỌC SẢN PHẨM ĐANG HOẠT ĐỘNG THEO HÃNG + GIÁ
    Page<SanPham> findByTrangThaiAndHangSanXuat_IdAndGiaLessThanEqual(String trangThai, Integer hangId, Integer maxPrice, Pageable pageable);

    // LỌC SẢN PHẨM ĐANG HOẠT ĐỘNG THEO LOẠI + HÃNG + GIÁ
    Page<SanPham> findByTrangThaiAndLoaiSanPham_TenLoaiAndHangSanXuat_IdAndGiaLessThanEqualOrderByIdDesc(
            String trangThai,
            String tenLoai,
            Integer hangId,
            Integer maxPrice,
            Pageable pageable
    );

    // LẤY SẢN PHẨM LIÊN QUAN ĐANG HOẠT ĐỘNG (Theo loại, trừ sản phẩm hiện tại)
    Page<SanPham> findByTrangThaiAndLoaiSanPham_IdAndIdNot(String trangThai, Integer loaiId, Integer excludeId, Pageable pageable);

    // TÌM KIẾM KẾT HỢP (Từ khóa + Hãng + Loại + Giá) + CHỈ HIỂN THỊ SẢN PHẨM ACTIVE
    // Cập nhật: Thêm cứng điều kiện s.trangThai = 'ACTIVE' để loại bỏ các sản phẩm đã xóa mềm (INACTIVE) khỏi trang Shop 
    // Nếu tham số truyền vào là NULL, điều kiện đó sẽ bị bỏ qua (luôn đúng)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM SanPham s WHERE " +
            "(s.trangThai = 'ACTIVE') AND " +
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

    // Tìm sản phẩm theo tên (không phân trang - dùng cho Admin)
    List<SanPham> findByTenSanPhamContainingIgnoreCase(String tenSanPham);

    // Lọc sản phẩm theo Hãng Sản Xuất (không phân trang - dùng cho Admin)
    List<SanPham> findByHangSanXuat_Id(Integer hangId);

    // Lọc sản phẩm theo cả Tên và Hãng Sản Xuất (không phân trang - Admin)
    List<SanPham> findByTenSanPhamContainingIgnoreCaseAndHangSanXuat_Id(String tenSanPham, Integer hangId);

    // LẤY SẢN PHẨM SẮP HẾT HÀNG (Sắp xếp theo số lượng tăng dần)
    List<SanPham> findTop5ByOrderBySoLuongAsc();
}