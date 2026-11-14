package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

public record MonHocDTO(String maMon, String tenMon, int soTinChi, Set<String> maMonTienQuyet) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public MonHocDTO {
        maMonTienQuyet = Set.copyOf(maMonTienQuyet);
    }
}

