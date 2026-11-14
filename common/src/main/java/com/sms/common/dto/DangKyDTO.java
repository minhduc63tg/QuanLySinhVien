package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record DangKyDTO(LopHocPhanDTO lopHocPhan, LocalDateTime ngayDangKy, Double diemSo) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

