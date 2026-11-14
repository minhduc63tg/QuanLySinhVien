package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import com.sms.common.enums.GioiTinh;
import com.sms.common.enums.TrangThaiHocTap;

public record SinhVienDTO(String maSv, String hoTen, LocalDate ngaySinh, GioiTinh gioiTinh,
        String email, String soDienThoai, TrangThaiHocTap trangThai,
        String maLop, String tenLop, String maKhoa, String tenKhoa) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

