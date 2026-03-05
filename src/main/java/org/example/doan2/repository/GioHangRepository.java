package org.example.doan2.repository;

import org.example.doan2.entity.GioHang;
import org.example.doan2.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository quản lý giao tiếp cơ sở dữ liệu cho bảng GioHang.
 * Chức năng: Lưu trữ thông tin tổng quát của giỏ hàng, kết nối giỏ hàng với một tài khoản người dùng cụ thể.
 */
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {
    
    /**
     * Tìm kiếm giỏ hàng của một người dùng.
     * Mục đích: Hệ thống quy định mỗi User chỉ có 1 giỏ hàng duy nhất đóng vai trò lưu trữ lâu dài.
     * @param nguoiDung Đối tượng người dùng đang đăng nhập
     * @return Optional chứa Giỏ hàng nếu đã từng được tạo, ngược lại trả về rỗng.
     */
    Optional<GioHang> findByNguoiDung(NguoiDung nguoiDung);
}
