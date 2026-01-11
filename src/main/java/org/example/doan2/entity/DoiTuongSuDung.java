package org.example.doan2.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "doi_tuong_su_dung")
@Getter
@Setter
@NoArgsConstructor
public class DoiTuongSuDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_doi_tuong", nullable = false, unique = true)
    private String tenDoiTuong;

    @Column(name = "mo_ta", nullable = false)
    private String moTa;
}
