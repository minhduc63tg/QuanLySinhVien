package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record ThoiKhoaBieuDTO(String hocKy, List<TietHocDTO> lichHoc) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public ThoiKhoaBieuDTO {
        lichHoc = List.copyOf(lichHoc);
    }
}

