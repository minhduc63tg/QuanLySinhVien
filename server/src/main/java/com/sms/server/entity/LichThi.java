package com.sms.server.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lịch thi của một lớp học phần (tối đa một lịch thi mỗi lớp) và các giảng viên gác thi. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lich_thi", uniqueConstraints = @UniqueConstraint(columnNames = "ma_lhp"))
@org.hibernate.annotations.Check(constraints = "tiet_bat_dau between 1 and 12 and so_tiet between 1 and 6 and tiet_bat_dau + so_tiet - 1 <= 12")
public class LichThi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_lhp", nullable = false)
    private LopHocPhan lopHocPhan;

    @Column(name = "ngay_thi", nullable = false)
    private LocalDate ngayThi;

    @Column(name = "tiet_bat_dau", nullable = false, columnDefinition = "tinyint")
    private int tietBatDau;

    @Column(name = "so_tiet", nullable = false, columnDefinition = "tinyint")
    private int soTiet;

    @Column(name = "phong", length = 20, nullable = false)
    private String phong;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "lich_thi_giam_thi",
            joinColumns = @JoinColumn(name = "id_lich_thi"),
            inverseJoinColumns = @JoinColumn(name = "ma_gv"))
    private Set<GiangVien> giamThi = new HashSet<>();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LichThi entity
                && getId() != null && getId().equals(entity.getId());
    }

    @Override
    public final int hashCode() {
        return LichThi.class.hashCode();
    }
}
