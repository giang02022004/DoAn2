package org.example.doan2.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "hinh_anh_san_pham")
@Getter
@Setter
@NoArgsConstructor
public class HinhAnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "duong_dan", nullable = false)
    private String duongDan;

    @ManyToOne
    @JoinColumn(name = "san_pham_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SanPham sanPham;
}
