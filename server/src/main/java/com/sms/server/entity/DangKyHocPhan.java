package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dang_ky_hoc_phan")
@org.hibernate.annotations.Check(constraints = "diem_so is null or diem_so between 0 and 10")
public class DangKyHocPhan {

    @EmbeddedId
    private DangKyId id;

    @MapsId("maSv")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_sv", nullable = false)
    private SinhVien sinhVien;

    @MapsId("maLhp")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_lhp", nullable = false)
    private LopHocPhan lopHocPhan;

    @Column(name = "ngay_dang_ky", nullable = false, columnDefinition = "datetime")
    private LocalDateTime ngayDangKy;

    @Column(name = "diem_so")
    private Double diemSo;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof DangKyHocPhan entity
                && getId() != null && getId().equals(entity.getId());
    }

    @Override
    public final int hashCode() {
        return DangKyHocPhan.class.hashCode();
    }
}

