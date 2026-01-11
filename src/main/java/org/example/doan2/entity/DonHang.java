package org.example.doan2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "don_hang")
@Getter
@Setter
@NoArgsConstructor
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tong_tien", nullable = false)
    private Integer tongTien;

    @Column(name = "ten_nguoi_nhan", nullable = false)
    private String tenNguoiNhan;

    @Column(name = "dien_thoai_nhan", nullable = false)
    private String dienThoaiNhan;

    @Column(name = "dia_chi_nhan", nullable = false, columnDefinition = "TEXT")
    private String diaChiNhan;

    @Column(name = "email_nhan")
    private String emailNhan;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "phuong_thuc_thanh_toan", nullable = false)
    private String phuongThucThanhToan;

    @Column(name = "trang_thai_thanh_toan", nullable = false)
    private String trangThaiThanhToan;

    @Column(name = "ma_thanh_toan")
    private String maThanhToan;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @ManyToOne
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;
}
