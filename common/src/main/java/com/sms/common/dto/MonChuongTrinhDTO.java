package com.sms.common.dto;

import com.sms.common.enums.TrangThaiMonKhung;
import java.io.Serial;
import java.io.Serializable;

/**
 * Một môn trong chương trình khung. trangThai và diemSo chỉ có giá trị khi trả về cho sinh viên
 * (tiến độ cá nhân); khi quản lý lưu chương trình, hai trường này bị bỏ qua.
 */
public record MonChuongTrinhDTO(String maMon, String tenMon, int soTinChi, int hocKyGoiY,
        boolean batBuoc, TrangThaiMonKhung trangThai, Double diemSo) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
