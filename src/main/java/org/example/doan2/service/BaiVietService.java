package org.example.doan2.service;

import org.example.doan2.entity.BaiViet;
import org.example.doan2.repository.BaiVietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BaiVietService {

    @Autowired
    private BaiVietRepository baiVietRepository;

    /**
     * Lấy danh sách bài viết có phân trang và lọc theo điều kiện:
     * 1. keyword: Tìm kiếm gần đúng (LIKE) theo Tiêu Đề Bài Viết.
     * 2. trangThai: Lọc theo trạng thái (ACTIVE/INACTIVE/DRAFT). Nếu là "ALL" thì lấy toàn bộ.
     * 3. sortBy: Sắp xếp theo một trường cụ thể (mặc định là ngayTao chiều giảm dần - bài mới nhất lên đầu).
     */
    public Page<BaiViet> getFilteredArticles(String keyword, String trangThai, int pageNo, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, sortBy));
        
        if (keyword != null && !keyword.isEmpty()) {
            return baiVietRepository.findByTieuDeContainingIgnoreCase(keyword, pageable);
        }
        
        if (trangThai != null && !trangThai.isEmpty() && !trangThai.equals("ALL")) {
            return baiVietRepository.findByTrangThai(trangThai, pageable);
        }
        
        return baiVietRepository.findAll(pageable);
    }
    
    /**
     * Lấy dữ liệu chi tiết của 1 bài viết dựa trên ID.
     * Dùng khi Admin bấm nút "Sửa" hoặc khi User muốn xem chi tiết bài đọc.
     */
    public BaiViet getArticleById(Integer id) {
        Optional<BaiViet> optional = baiVietRepository.findById(id);
        return optional.orElse(null);
    }

    /**
     * Lưu bài viết mới hoặc Lưu bài viết sau khi được cập nhật nội dung.
     */
    public void saveArticle(BaiViet baiViet) {
        baiVietRepository.save(baiViet);
    }

    /**
     * Xóa vĩnh viễn bài viết khỏi Cơ sở dữ liệu bằng ID.
     */
    public void deleteArticle(Integer id) {
        baiVietRepository.deleteById(id);
    }
}
