package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import com.sms.common.enums.ErrorCode;

/** Mã lỗi và thông báo giải thích khi lớp không đủ điều kiện đăng ký. */
public record LopCoTheDangKyDTO(LopHocPhanDTO lopHocPhan, boolean coTheDangKy,
        ErrorCode lyDoKhongDangKy, String thongBao) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

