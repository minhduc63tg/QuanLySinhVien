package com.sms.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mon_hoc")
@org.hibernate.annotations.Check(constraints = "so_tin_chi between 1 and 10")
public class MonHoc {

    @Id
    @Column(name = "ma_mon", length = 20, nullable = false)
    private String maMon;

    @Column(name = "ten_mon", length = 150, nullable = false)
    private String tenMon;

    @Column(name = "so_tin_chi", nullable = false)
    private int soTinChi;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "mon_tien_quyet",
            joinColumns = @JoinColumn(name = "ma_mon_chinh"),
            inverseJoinColumns = @JoinColumn(name = "ma_mon_tien_quyet"))
    private Set<MonHoc> monTienQuyet = new HashSet<>();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof MonHoc entity
                && getMaMon() != null && getMaMon().equals(entity.getMaMon());
    }

    @Override
    public final int hashCode() {
        return MonHoc.class.hashCode();
    }
}

