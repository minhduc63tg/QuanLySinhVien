package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


public record KhoaDTO(String maKhoa, String tenKhoa) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

