package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Chương trình khung của một khoa (khối ngành) cùng tổng số tín chỉ cần tích lũy để tốt nghiệp. */
public record ChuongTrinhKhungDTO(String maKhoa, String tenKhoa, int tongTinChi,
        List<MonChuongTrinhDTO> monHoc) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
