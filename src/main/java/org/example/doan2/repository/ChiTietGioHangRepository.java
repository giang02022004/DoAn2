package org.example.doan2.repository;

import org.example.doan2.entity.ChiTietGioHang;
import org.example.doan2.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý giao tiếp cơ sở dữ liệu cho bảng ChiTietGioHang.
 * Chức năng: Lưu trữ danh sách từng món hàng cụ thể (Sản phẩm mẹ, Biến thể con, Số lượng, Đơn giá) 
 * đang nằm bên trong một Giỏ hàng cha.
 */
public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, Integer> {
    
    /**
     * Lấy danh sách toàn bộ mặt hàng có trong một giỏ hàng cụ thể để hiển thị ra giao diện.
     */
    List<ChiTietGioHang> findByGioHang(GioHang gioHang);
    
    /**
     * Tìm một món hàng đang có trong giỏ, dùng cho SẢN PHẨM CÓ BIẾN THỂ (VD: Laptop màu Đen, RAM 16GB).
     * Mục đích: Khi khách thêm hàng, phải kiểm tra xem hàng này đã có chưa để "cộng dồn số lượng" thay vì tạo ra 2 dòng giống hệt nhau.
     */
    Optional<ChiTietGioHang> findByGioHangAndSanPhamIdAndBienTheId(GioHang gioHang, Integer sanPhamId, Integer bienTheId);
    
    /**
     * Tìm một món hàng đang có trong giỏ, dùng cho SẢN PHẨM KHÔNG CÓ BIẾN THỂ (VD: Trái cây, Phụ kiện).
     * Mục đích: Tương tự như trên, tìm để cộng dồn số lượng nếu thao tác Thêm vào giỏ lặp lại nhiều lần.
     */
    Optional<ChiTietGioHang> findByGioHangAndSanPhamIdAndBienTheIsNull(GioHang gioHang, Integer sanPhamId);
}
