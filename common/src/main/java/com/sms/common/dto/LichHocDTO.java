package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


public record LichHocDTO(Long id, int thu, int tietBatDau, int soTiet, String phong) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

