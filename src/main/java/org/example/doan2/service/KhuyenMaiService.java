package org.example.doan2.service;

import org.example.doan2.entity.KhuyenMai;
import org.example.doan2.repository.KhuyenMaiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;

    /**
     * Lấy danh sách các chương trình khuyến mãi có phân trang.
     * Hỗ trợ tìm kiếm theo tên chương trình nếu có từ khóa.
     * 
     * @param keyword Từ khóa tìm kiếm (tên khuyến mãi)
     * @param pageable Đối tượng phân trang (số trang, kích thước trang)
     * @return Danh sách khuyến mãi được phân trang
     */
    public Page<KhuyenMai> getKhuyenMais(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            // Tìm kiếm theo tên nếu có từ khóa
            return khuyenMaiRepository.searchByTen(keyword, pageable);
        }
        // Trả về toàn bộ danh sách khuyến mãi đang hoạt động (không bị xóa)
        return khuyenMaiRepository.findAllActive(pageable);
    }

    /**
     * Lấy danh sách tất cả các khuyến mãi đang trong thời hạn hiệu lực.
     * Thường dùng để hiển thị các chương trình đang chạy cho khách hàng hoặc đổ vào dropdown chọn khuyến mãi.
     */
    public List<KhuyenMai> getAllAvailable() {
        return khuyenMaiRepository.findAllAvailable();
    }

    /**
     * Lưu thông tin chương trình khuyến mãi mới hoặc cập nhật thông tin cũ.
     * Tự động thiết lập trạng thái "ACTIVE" nếu không được chỉ định.
     */
    public KhuyenMai saveKhuyenMai(KhuyenMai khuyenMai) {
        if (khuyenMai.getTrangThai() == null || khuyenMai.getTrangThai().isEmpty()) {
            khuyenMai.setTrangThai("ACTIVE");
        }
        return khuyenMaiRepository.save(khuyenMai);
    }

    /**
     * Truy vấn thông tin chi tiết một chương trình khuyến mãi theo ID.
     */
    public KhuyenMai getKhuyenMaiById(Integer id) {
        Optional<KhuyenMai> optional = khuyenMaiRepository.findById(id);
        return optional.orElse(null);
    }

    /**
     * Xử lý xóa chương trình khuyến mãi.
     * Sử dụng cơ chế Xóa mềm (Soft Delete): Không xóa bản ghi khỏi DB mà chỉ chuyển trạng thái sang "DELETED".
     * Điều này giúp bảo toàn dữ liệu lịch sử (ví dụ: các đơn hàng cũ đã áp dụng KM này).
     */
    public void deleteKhuyenMai(Integer id) {
        KhuyenMai km = getKhuyenMaiById(id);
        if (km != null) {
            km.setTrangThai("DELETED"); // Đánh dấu là đã xóa
            khuyenMaiRepository.save(km);
        }
    }
}
