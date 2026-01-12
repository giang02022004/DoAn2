package org.example.doan2.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name = "san_pham")
@Getter
@Setter
@NoArgsConstructor
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @Column(nullable = false)
    private Integer gia;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "mo_ta_chi_tiet", columnDefinition = "TEXT", nullable = false)
    private String moTaChiTiet;

    @Column(name = "mo_ta_ngan", nullable = false)
    private String moTaNgan;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "da_ban")
    private Integer daBan;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @ManyToOne
    @JoinColumn(name = "hang_sx_id", nullable = false)
    private HangSanXuat hangSanXuat;
    @ManyToOne
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loai_san_pham_id", nullable = false)
    private LoaiSanPham loaiSanPham;
}
