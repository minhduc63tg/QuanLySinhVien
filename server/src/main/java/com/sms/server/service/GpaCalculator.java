package com.sms.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GpaCalculator {

    private GpaCalculator() {
    }

    public record KetQuaQuyDoi(String diemChu, double diemHe4, boolean dat) {
    }

    public record DiemMon(String maMon, int soTinChi, Double diemSo) {
    }

    public static double lamTron(double diem) {
        if (!Double.isFinite(diem) || diem < 0 || diem > 10) {
            throw new IllegalArgumentException("Điểm phải nằm trong [0, 10]");
        }
        return BigDecimal.valueOf(diem).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static KetQuaQuyDoi quyDoi(double diem10) {
        if (!Double.isFinite(diem10) || diem10 < 0 || diem10 > 10) {
            throw new IllegalArgumentException("Điểm phải nằm trong [0, 10]");
        }
        if (diem10 >= 9.0) return new KetQuaQuyDoi("A+", 4.0, true);
        if (diem10 >= 8.5) return new KetQuaQuyDoi("A", 3.8, true);
        if (diem10 >= 8.0) return new KetQuaQuyDoi("B+", 3.5, true);
        if (diem10 >= 7.0) return new KetQuaQuyDoi("B", 3.0, true);
        if (diem10 >= 6.0) return new KetQuaQuyDoi("C+", 2.5, true);
        if (diem10 >= 5.5) return new KetQuaQuyDoi("C", 2.0, true);
        if (diem10 >= 5.0) return new KetQuaQuyDoi("D+", 1.5, true);
        if (diem10 >= 4.0) return new KetQuaQuyDoi("D", 1.0, true);
        return new KetQuaQuyDoi("F", 0.0, false);
    }

    /** Mỗi môn chỉ tính lần có điểm cao nhất; chưa có điểm trả null. */
    public static Double tinhGpa(Collection<DiemMon> scores) {
        Map<String, DiemMon> best = new HashMap<>();
        for (DiemMon score : scores) {
            if (score.diemSo() == null) continue;
            if (score.soTinChi() < 1) throw new IllegalArgumentException("Số tín chỉ phải dương");
            quyDoi(score.diemSo());
            best.merge(score.maMon(), score,
                    (a, b) -> a.diemSo() >= b.diemSo() ? a : b);
        }
        if (best.isEmpty()) return null;
        double total = 0;
        int credits = 0;
        for (DiemMon score : best.values()) {
            total += quyDoi(score.diemSo()).diemHe4() * score.soTinChi();
            credits += score.soTinChi();
        }
        return total / credits;
    }
}
