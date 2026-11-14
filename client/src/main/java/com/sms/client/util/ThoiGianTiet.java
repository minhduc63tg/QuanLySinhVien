package com.sms.client.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Quy ước giờ học (chỉ để hiển thị). Mỗi tiết 50 phút; 12 tiết chia thành ba buổi:
 * sáng (tiết 1-5, từ 07:00), chiều (tiết 6-10, từ 13:00) và tối (tiết 11-12, từ 18:00).
 */
public final class ThoiGianTiet {

    public enum Buoi {
        SANG("Sáng", "cal-sang"),
        CHIEU("Chiều", "cal-chieu"),
        TOI("Tối", "cal-toi");

        private final String ten;
        private final String styleClass;

        Buoi(String ten, String styleClass) {
            this.ten = ten;
            this.styleClass = styleClass;
        }

        public String ten() {
            return ten;
        }

        /** Tên lớp CSS tô màu của buổi (xem app.css). */
        public String styleClass() {
            return styleClass;
        }
    }

    public static final int PHUT_MOI_TIET = 50;
    public static final int SO_TIET_TOI_DA = 12;

    /** Giờ bắt đầu của từng tiết 1..12. */
    private static final List<LocalTime> BAT_DAU = List.of(
            LocalTime.of(7, 0), LocalTime.of(7, 50), LocalTime.of(8, 40), LocalTime.of(9, 30), LocalTime.of(10, 20),
            LocalTime.of(13, 0), LocalTime.of(13, 50), LocalTime.of(14, 40), LocalTime.of(15, 30), LocalTime.of(16, 20),
            LocalTime.of(18, 0), LocalTime.of(18, 50));

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private ThoiGianTiet() {
    }

    public static String batDau(int tiet) {
        return HM.format(BAT_DAU.get(tiet - 1));
    }

    public static String ketThuc(int tiet) {
        return HM.format(BAT_DAU.get(tiet - 1).plusMinutes(PHUT_MOI_TIET));
    }

    /** Khoảng giờ của một đoạn tiết liên tiếp, ví dụ "07:00 - 09:30". */
    public static String khoangGio(int tietBatDau, int soTiet) {
        return batDau(tietBatDau) + " - " + ketThuc(tietBatDau + soTiet - 1);
    }

    /** Buổi của một tiết (tính theo tiết bắt đầu của lịch). */
    public static Buoi buoi(int tiet) {
        return tiet <= 5 ? Buoi.SANG : tiet <= 10 ? Buoi.CHIEU : Buoi.TOI;
    }
}
