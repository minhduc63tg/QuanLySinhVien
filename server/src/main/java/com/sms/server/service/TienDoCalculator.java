package com.sms.server.service;

import com.sms.common.dto.ChuongTrinhKhungDTO;
import com.sms.common.dto.MonChuongTrinhDTO;
import com.sms.common.dto.TienDoHocTapDTO;
import com.sms.common.enums.TrangThaiMonKhung;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tính trạng thái từng môn và tiến độ tín chỉ so với chương trình khung (logic thuần, có unit test). */
final class TienDoCalculator {

    private TienDoCalculator() {
    }

    /**
     * @param chuongTrinh chương trình khung (trangThai/diemSo trong các môn bị bỏ qua)
     * @param diemTheoMon mọi lần học của sinh viên theo mã môn; phần tử null là lần đang học chưa có điểm
     * @param diemQua     ngưỡng điểm đạt (điểm &gt;= diemQua là đạt)
     */
    static TienDoHocTapDTO tinh(ChuongTrinhKhungDTO chuongTrinh, Map<String, List<Double>> diemTheoMon,
            double diemQua) {
        List<MonChuongTrinhDTO> monHoc = new ArrayList<>();
        int tichLuy = 0;
        int dangHoc = 0;
        int batBuocYeuCau = 0;
        int batBuocTichLuy = 0;
        for (MonChuongTrinhDTO mon : chuongTrinh.monHoc()) {
            List<Double> attempts = diemTheoMon.getOrDefault(mon.maMon(), List.of());
            Double best = attempts.stream().filter(d -> d != null).max(Double::compare).orElse(null);
            TrangThaiMonKhung status = trangThai(attempts, best, diemQua);
            monHoc.add(new MonChuongTrinhDTO(mon.maMon(), mon.tenMon(), mon.soTinChi(), mon.hocKyGoiY(),
                    mon.batBuoc(), status, best));
            if (mon.batBuoc()) {
                batBuocYeuCau += mon.soTinChi();
            }
            if (status == TrangThaiMonKhung.HOAN_THANH) {
                tichLuy += mon.soTinChi();
                if (mon.batBuoc()) {
                    batBuocTichLuy += mon.soTinChi();
                }
            } else if (status == TrangThaiMonKhung.DANG_HOC) {
                dangHoc += mon.soTinChi();
            }
        }
        return new TienDoHocTapDTO(new ChuongTrinhKhungDTO(chuongTrinh.maKhoa(), chuongTrinh.tenKhoa(),
                chuongTrinh.tongTinChi(), List.copyOf(monHoc)),
                chuongTrinh.tongTinChi(), tichLuy, dangHoc, batBuocYeuCau, batBuocTichLuy);
    }

    /** Ưu tiên: đã đạt &gt; đang học &gt; có điểm nhưng chưa đạt &gt; chưa học. */
    static TrangThaiMonKhung trangThai(List<Double> attempts, Double best, double diemQua) {
        if (best != null && best >= diemQua) {
            return TrangThaiMonKhung.HOAN_THANH;
        }
        if (attempts.stream().anyMatch(d -> d == null)) {
            return TrangThaiMonKhung.DANG_HOC;
        }
        if (best != null) {
            return TrangThaiMonKhung.CHUA_DAT;
        }
        return TrangThaiMonKhung.CHUA_HOC;
    }
}
