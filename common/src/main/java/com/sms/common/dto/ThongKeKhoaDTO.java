package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


public record ThongKeKhoaDTO(String maKhoa, String tenKhoa, long soSinhVien) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

