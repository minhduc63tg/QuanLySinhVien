package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lop_hoc_phan")
@org.hibernate.annotations.Check(constraints = "so_luong_toi_da > 0 and so_luong_da_dk between 0 and so_luong_toi_da")
public class LopHocPhan {

    @Id
    @Column(name = "ma_lhp", length = 30, nullable = false)
    private String maLhp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_mon", nullable = false)
    private MonHoc monHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_gv")
    private GiangVien giangVien;

    @Column(name = "hoc_ky", length = 20, nullable = false)
    private String hocKy;

    @Column(name = "so_luong_toi_da", nullable = false)
    private int soLuongToiDa;

    @Column(name = "so_luong_da_dk", nullable = false, columnDefinition = "integer default 0")
    private int soLuongDaDk;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(mappedBy = "lopHocPhan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LichHoc> lichHoc = new ArrayList<>();

    public void themLichHoc(LichHoc lich) {
        lichHoc.add(lich);
        lich.setLopHocPhan(this);
    }

    public void xoaLichHoc(LichHoc lich) {
        if (lichHoc.remove(lich)) {
            lich.setLopHocPhan(null);
        }
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LopHocPhan entity
                && getMaLhp() != null && getMaLhp().equals(entity.getMaLhp());
    }

    @Override
    public final int hashCode() {
        return LopHocPhan.class.hashCode();
    }
}

