package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import com.sms.common.enums.TrangThaiHocTap;

public record ThongKeTrangThaiDTO(TrangThaiHocTap trangThai, long soSinhVien) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

