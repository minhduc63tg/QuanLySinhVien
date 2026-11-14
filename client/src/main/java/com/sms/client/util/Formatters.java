package com.sms.client.util;

import com.sms.common.enums.VaiTro;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Định dạng hiển thị: ngày dd/MM/yyyy, điểm 1 chữ số thập phân. */
public final class Formatters {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatters() {
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String diem(Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.1f", value);
    }

    public static String gpa(Double value) {
        return value == null ? "chưa có" : String.format(Locale.ROOT, "%.2f", value);
    }

    public static String gioiTinh(com.sms.common.enums.GioiTinh value) {
        return switch (value) {
            case NAM -> "Nam";
            case NU -> "Nữ";
            case KHAC -> "Khác";
        };
    }

    public static String trangThai(com.sms.common.enums.TrangThaiHocTap value) {
        return switch (value) {
            case DANG_HOC -> "Đang học";
            case CANH_BAO -> "Cảnh báo";
            case TOT_NGHIEP -> "Tốt nghiệp";
        };
    }

    public static String vaiTro(VaiTro role) {
        return switch (role) {
            case SINH_VIEN -> "Sinh viên";
            case GIANG_VIEN -> "Giảng viên";
            case QUAN_LY -> "Quản lý";
        };
    }
}
