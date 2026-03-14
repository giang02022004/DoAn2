package org.example.doan2.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "khuyen_mai")
public class KhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_khuyen_mai")
    private String tenKhuyenMai;

    @Column(name = "phan_tram_giam")
    private int phanTramGiam;

    @Column(name = "ngay_bat_dau")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime ngayKetThuc;

    @Column(name = "trang_thai")
    private String trangThai;

    /** ─── NGHIỆP VỤ: Kiểm tra Khuyến mãi có đang hiệu lực không ─── */
    public boolean isHieuLuc() {
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equals(this.trangThai)
                && (ngayBatDau == null || now.isAfter(ngayBatDau))
                && (ngayKetThuc == null || now.isBefore(ngayKetThuc));
    }
}
