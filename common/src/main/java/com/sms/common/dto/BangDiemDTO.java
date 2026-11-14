package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** GPA để null nếu chưa có môn được chấm điểm. */
public record BangDiemDTO(List<BangDiemHocKyDTO> hocKy, Double gpaTichLuy) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public BangDiemDTO {
        hocKy = List.copyOf(hocKy);
    }
}

