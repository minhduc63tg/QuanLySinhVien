package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** GPA để null nếu chưa có môn được chấm điểm. */
public record BangDiemHocKyDTO(String hocKy, List<DiemDTO> bangDiem, Double gpa) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public BangDiemHocKyDTO {
        bangDiem = List.copyOf(bangDiem);
    }
}

