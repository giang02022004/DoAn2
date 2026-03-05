package org.example.doan2.repository;

import org.example.doan2.entity.DonHang;
import org.example.doan2.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {
    List<DonHang> findByNguoiDungOrderByNgayTaoDesc(NguoiDung nguoiDung);

    // Tính tổng doanh thu từ tất cả các đơn hàng (Cộng dồn cột tongTien, nếu null thì trả về 0)
    @Query("SELECT COALESCE(SUM(d.tongTien), 0) FROM DonHang d")
    Integer sumTotalRevenue();

    // Lấy danh sách 5 đơn hàng mới nhất (Sắp xếp theo ngày tạo giảm dần - Descending)
    List<DonHang> findTop5ByOrderByNgayTaoDesc();

    // Lấy TẤT CẢ đơn hàng để hiển thị trên trang Quản lý Đơn hàng (Sắp xếp từ mới đến cũ)
    List<DonHang> findAllByOrderByNgayTaoDesc();

    // Lấy danh sách đơn hàng lọc theo trạng thái (Sắp xếp mới nhất)
    List<DonHang> findByTrangThaiOrderByNgayTaoDesc(String trangThai);

    /**
     * Thống kê doanh thu theo từng THÁNG trong 12 tháng gần nhất.
     * Truy vấn nhóm các đơn hàng lại theo Năm + Tháng, rồi tính tổng tongTien.
     * Dữ liệu trả về dạng Object[]: [năm (Integer), tháng (Integer), tổng_doanh_thu (Long)]
     */
    @Query("SELECT YEAR(d.ngayTao), MONTH(d.ngayTao), COALESCE(SUM(d.tongTien), 0) " +
           "FROM DonHang d " +
           "GROUP BY YEAR(d.ngayTao), MONTH(d.ngayTao) " +
           "ORDER BY YEAR(d.ngayTao) ASC, MONTH(d.ngayTao) ASC")
    List<Object[]> getMonthlyRevenue();

    /**
     * Thống kê số lượng đơn hàng theo từng THÁNG trong 12 tháng gần nhất.
     * Dữ liệu trả về dạng Object[]: [năm (Integer), tháng (Integer), số_đơn (Long)]
     */
    @Query("SELECT YEAR(d.ngayTao), MONTH(d.ngayTao), COUNT(d) " +
           "FROM DonHang d " +
           "GROUP BY YEAR(d.ngayTao), MONTH(d.ngayTao) " +
           "ORDER BY YEAR(d.ngayTao) ASC, MONTH(d.ngayTao) ASC")
    List<Object[]> getMonthlyOrderCount();
}
