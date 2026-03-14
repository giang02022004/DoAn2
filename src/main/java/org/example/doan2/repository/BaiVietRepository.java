package org.example.doan2.repository;

import org.example.doan2.entity.BaiViet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaiVietRepository extends JpaRepository<BaiViet, Integer> {
    
    // Tìm tiếm theo tiêu đề (cho ô search)
    Page<BaiViet> findByTieuDeContainingIgnoreCase(String keyword, Pageable pageable);
    
    // Lấy danh sách các bài viết đang hiển thị (ACTIVE) dùng cho trang User sau này
    @Query("SELECT b FROM BaiViet b WHERE b.trangThai = 'ACTIVE' ORDER BY b.ngayTao DESC")
    List<BaiViet> findActiveArticles();

    // Tìm kiếm theo trạng thái (tùy chọn mở rộng sau này)
    Page<BaiViet> findByTrangThai(String trangThai, Pageable pageable);
}
