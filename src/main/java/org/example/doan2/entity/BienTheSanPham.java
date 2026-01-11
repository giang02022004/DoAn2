package org.example.doan2.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "bien_the_san_pham")
@Getter
@Setter
@NoArgsConstructor
public class BienTheSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String cpu;

    @Column(name = "bo_nho", nullable = false)
    private String boNho;

    @Column(name = "gia_them")
    private Integer giaThem;

    @Column(name = "mau_sac", nullable = false)
    private String mauSac;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "da_ban")
    private Integer daBan;

    @ManyToOne
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;
}
