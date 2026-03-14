package org.example.doan2.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bai_viet")
public class BaiViet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;

    @Column(name = "noi_dung_ngan", length = 500)
    private String noiDungNgan;

    @Column(name = "noi_dung_chi_tiet", columnDefinition = "TEXT")
    private String noiDungChiTiet;

    @Column(name = "hinh_anh", length = 255)
    private String hinhAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung tacGia;

    @Column(name = "trang_thai", length = 50)
    private String trangThai; // "ACTIVE" hoặc "INACTIVE" hoặc "DRAFT"

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    // Default constructor
    public BaiViet() {
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDungNgan() {
        return noiDungNgan;
    }

    public void setNoiDungNgan(String noiDungNgan) {
        this.noiDungNgan = noiDungNgan;
    }

    public String getNoiDungChiTiet() {
        return noiDungChiTiet;
    }

    public void setNoiDungChiTiet(String noiDungChiTiet) {
        this.noiDungChiTiet = noiDungChiTiet;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public NguoiDung getTacGia() {
        return tacGia;
    }

    public void setTacGia(NguoiDung tacGia) {
        this.tacGia = tacGia;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }
}
