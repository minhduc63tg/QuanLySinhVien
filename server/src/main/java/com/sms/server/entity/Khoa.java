package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "khoa")
public class Khoa {

    @Id
    @Column(name = "ma_khoa", length = 20, nullable = false)
    private String maKhoa;

    @Column(name = "ten_khoa", length = 100, nullable = false)
    private String tenKhoa;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Khoa entity
                && getMaKhoa() != null && getMaKhoa().equals(entity.getMaKhoa());
    }

    @Override
    public final int hashCode() {
        return Khoa.class.hashCode();
    }
}

