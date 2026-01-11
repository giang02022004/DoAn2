package org.example.doan2.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "hang_san_xuat")
@Getter
@Setter
@NoArgsConstructor
public class HangSanXuat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_hang", nullable = false, unique = true)
    private String tenHang;

    @Column(name = "mo_ta", nullable = false)
    private String moTa;
}
