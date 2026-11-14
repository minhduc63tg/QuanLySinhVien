package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record LopHocPhanDTO(String maLhp, String maMon, String tenMon, int soTinChi,
        String maGv, String tenGiangVien, String hocKy, int soLuongToiDa,
        int soLuongDaDk, int version, List<LichHocDTO> lichHoc) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public LopHocPhanDTO {
        lichHoc = List.copyOf(lichHoc);
    }
}

