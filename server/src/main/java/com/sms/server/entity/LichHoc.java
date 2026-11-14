package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lich_hoc")
@org.hibernate.annotations.Check(constraints = "thu between 2 and 8 and tiet_bat_dau between 1 and 12 and so_tiet between 1 and 6 and tiet_bat_dau + so_tiet - 1 <= 12")
public class LichHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_lhp", nullable = false)
    private LopHocPhan lopHocPhan;

    @Column(name = "thu", nullable = false, columnDefinition = "tinyint")
    private int thu;

    @Column(name = "tiet_bat_dau", nullable = false, columnDefinition = "tinyint")
    private int tietBatDau;

    @Column(name = "so_tiet", nullable = false, columnDefinition = "tinyint")
    private int soTiet;

    @Column(name = "phong", length = 20, nullable = false)
    private String phong;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LichHoc entity
                && getId() != null && getId().equals(entity.getId());
    }

    @Override
    public final int hashCode() {
        return LichHoc.class.hashCode();
    }
}

