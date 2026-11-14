package com.sms.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DangKyId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "ma_sv", length = 20, nullable = false)
    private String maSv;

    @Column(name = "ma_lhp", length = 30, nullable = false)
    private String maLhp;

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DangKyId id
                && Objects.equals(maSv, id.maSv) && Objects.equals(maLhp, id.maLhp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maSv, maLhp);
    }
}

