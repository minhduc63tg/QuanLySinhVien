package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import com.sms.common.enums.GioiTinh;
import com.sms.common.enums.TrangThaiHocTap;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sinh_vien")
public class SinhVien {

    @Id
    @Column(name = "ma_sv", length = 20, nullable = false)
    private String maSv;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ma_sv", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ho_ten", length = 100, nullable = false)
    private String hoTen;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "gioi_tinh", length = 10)
    private GioiTinh gioiTinh;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", length = 15)
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "trang_thai", length = 20, nullable = false, columnDefinition = "varchar(20) default 'DANG_HOC'")
    private TrangThaiHocTap trangThai = TrangThaiHocTap.DANG_HOC;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_lop", nullable = false)
    private LopHoc lopHoc;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SinhVien entity
                && getMaSv() != null && getMaSv().equals(entity.getMaSv());
    }

    @Override
    public final int hashCode() {
        return SinhVien.class.hashCode();
    }
}

