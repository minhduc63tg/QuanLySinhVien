package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một môn trong chương trình khung: học kỳ gợi ý và bắt buộc/tự chọn. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chuong_trinh_khung_mon",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ma_khoa", "ma_mon"}))
@org.hibernate.annotations.Check(constraints = "hoc_ky_goi_y between 1 and 12")
public class ChuongTrinhKhungMon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_khoa", nullable = false)
    private ChuongTrinhKhung chuongTrinh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_mon", nullable = false)
    private MonHoc monHoc;

    @Column(name = "hoc_ky_goi_y", nullable = false, columnDefinition = "tinyint")
    private int hocKyGoiY;

    @Column(name = "bat_buoc", nullable = false)
    private boolean batBuoc;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ChuongTrinhKhungMon entity
                && getId() != null && getId().equals(entity.getId());
    }

    @Override
    public final int hashCode() {
        return ChuongTrinhKhungMon.class.hashCode();
    }
}
