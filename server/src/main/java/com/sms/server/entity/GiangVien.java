package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "giang_vien")
public class GiangVien {

    @Id
    @Column(name = "ma_gv", length = 20, nullable = false)
    private String maGv;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ma_gv", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ho_ten", length = 100, nullable = false)
    private String hoTen;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", length = 15)
    private String soDienThoai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khoa")
    private Khoa khoa;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof GiangVien entity
                && getMaGv() != null && getMaGv().equals(entity.getMaGv());
    }

    @Override
    public final int hashCode() {
        return GiangVien.class.hashCode();
    }
}

