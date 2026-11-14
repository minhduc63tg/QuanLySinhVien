package com.sms.server.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chương trình khung của một khoa (một khoa có tối đa một chương trình). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chuong_trinh_khung")
@org.hibernate.annotations.Check(constraints = "tong_tin_chi between 0 and 500")
public class ChuongTrinhKhung {

    @Id
    @Column(name = "ma_khoa", length = 20, nullable = false)
    private String maKhoa;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ma_khoa")
    private Khoa khoa;

    @Column(name = "tong_tin_chi", nullable = false)
    private int tongTinChi;

    @OneToMany(mappedBy = "chuongTrinh", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChuongTrinhKhungMon> monHoc = new ArrayList<>();

    public void themMon(ChuongTrinhKhungMon mon) {
        mon.setChuongTrinh(this);
        monHoc.add(mon);
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ChuongTrinhKhung entity
                && getMaKhoa() != null && getMaKhoa().equals(entity.getMaKhoa());
    }

    @Override
    public final int hashCode() {
        return ChuongTrinhKhung.class.hashCode();
    }
}
