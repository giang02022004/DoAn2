import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
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

    @Column(name = "mo_ta_chi_tiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    @Column(name = "mo_ta_ngan")
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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loai_san_pham_id", nullable = false)
    private LoaiSanPham loaiSanPham;

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    private List<HinhAnhSanPham> danhSachHinhAnh;

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BienTheSanPham> bienTheList;

    /** ─── NGHIỆP VỤ: Kiểm tra sản phẩm có đang trong đợt giảm giá không ─── */
    public boolean isDangKhuyenMai() {
        return khuyenMai != null && khuyenMai.isHieuLuc();
    }

    /** ─── NGHIỆP VỤ: Lấy giá bán cuối cùng (đã trừ % giảm nếu có KM) ─── */
    public int getGiaDaGiam() {
        if (isDangKhuyenMai()) {
            return gia - (gia * khuyenMai.getPhanTramGiam() / 100);
        }
        return gia;
    }
}
