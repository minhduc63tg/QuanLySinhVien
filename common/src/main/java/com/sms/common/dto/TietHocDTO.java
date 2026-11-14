package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


public record TietHocDTO(String maLhp, String maMon, String tenMon, String maGv,
        String tenGiangVien, int thu, int tietBatDau, int soTiet, String phong) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

