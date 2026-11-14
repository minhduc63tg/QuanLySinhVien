package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


public record SiSoLopHocPhanDTO(String maLhp, String tenMon, String hocKy, int soLuongDaDk, int soLuongToiDa) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

