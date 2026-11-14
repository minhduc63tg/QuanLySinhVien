package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;


/** Các trường điểm và kết quả để null khi chưa có điểm. */
public record DiemDTO(String maSv, String hoTen, String maLhp, String maMon, String tenMon,
        int soTinChi, String hocKy, Double diemSo, String diemChu, Double diemHe4,
        Boolean dat) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

