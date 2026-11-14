package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lop_hoc")
public class LopHoc {

    @Id
    @Column(name = "ma_lop", length = 20, nullable = false)
    private String maLop;

    @Column(name = "ten_lop", length = 100, nullable = false)
    private String tenLop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_khoa", nullable = false)
    private Khoa khoa;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LopHoc entity
                && getMaLop() != null && getMaLop().equals(entity.getMaLop());
    }

    @Override
    public final int hashCode() {
        return LopHoc.class.hashCode();
    }
}

