package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.sms.common.enums.VaiTro;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tai_khoan")
public class TaiKhoan {

    @Id
    @Column(name = "ten_dang_nhap", length = 20, nullable = false)
    private String tenDangNhap;

    @Column(name = "mat_khau", length = 60, nullable = false)
    private String matKhau;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "vai_tro", length = 20, nullable = false)
    private VaiTro vaiTro;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TaiKhoan entity
                && getTenDangNhap() != null && getTenDangNhap().equals(entity.getTenDangNhap());
    }

    @Override
    public final int hashCode() {
        return TaiKhoan.class.hashCode();
    }
}

