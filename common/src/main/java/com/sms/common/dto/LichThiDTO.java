package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Lịch thi của một lớp học phần (mỗi lớp có tối đa một lịch thi). Khi lưu, server chỉ dùng maLhp,
 * ngayThi, tietBatDau, soTiet, phong và maGiamThi; các trường còn lại do server điền khi trả về.
 * maGiamThi và tenGiamThi là các giảng viên gác thi, cùng thứ tự.
 */
public record LichThiDTO(String maLhp, String maMon, String tenMon, String hocKy, LocalDate ngayThi,
        int tietBatDau, int soTiet, String phong, List<String> maGiamThi, List<String> tenGiamThi,
        int soThiSinh) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
