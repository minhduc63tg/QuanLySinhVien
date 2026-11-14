package com.sms.server.service;

import com.sms.common.dto.ChuongTrinhKhungDTO;
import com.sms.common.dto.MonChuongTrinhDTO;
import com.sms.common.dto.TienDoHocTapDTO;
import com.sms.common.enums.TrangThaiMonKhung;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TienDoCalculatorTest {

    private static MonChuongTrinhDTO mon(String ma, int tinChi, int hocKy, boolean batBuoc) {
        return new MonChuongTrinhDTO(ma, "Môn " + ma, tinChi, hocKy, batBuoc, null, null);
    }

    private static List<Double> lan(Double... diem) {
        return new ArrayList<>(Arrays.asList(diem));
    }

    private static final ChuongTrinhKhungDTO CHUONG_TRINH = new ChuongTrinhKhungDTO("K", "Khoa", 130, List.of(
            mon("A", 3, 1, true), mon("B", 4, 1, true), mon("C", 3, 2, true),
            mon("D", 2, 2, false), mon("E", 3, 3, true)));

    @Test
    void classifiesEachCourseAndSumsCredits() {
        TienDoHocTapDTO progress = TienDoCalculator.tinh(CHUONG_TRINH, Map.of(
                "A", lan(8.0),            // đạt
                "B", lan(3.9),            // có điểm nhưng chưa đạt
                "C", lan((Double) null),  // đang học
                "D", lan(4.0)),           // đạt đúng ngưỡng, môn tự chọn
                4.0);
        List<MonChuongTrinhDTO> items = progress.chuongTrinh().monHoc();
        assertEquals(TrangThaiMonKhung.HOAN_THANH, items.get(0).trangThai());
        assertEquals(TrangThaiMonKhung.CHUA_DAT, items.get(1).trangThai());
        assertEquals(TrangThaiMonKhung.DANG_HOC, items.get(2).trangThai());
        assertEquals(TrangThaiMonKhung.HOAN_THANH, items.get(3).trangThai());
        assertEquals(TrangThaiMonKhung.CHUA_HOC, items.get(4).trangThai());
        assertEquals(8.0, items.get(0).diemSo());
        assertNull(items.get(4).diemSo());

        assertEquals(130, progress.tinChiYeuCau());
        assertEquals(5, progress.tinChiTichLuy());      // A(3) + D(2)
        assertEquals(3, progress.tinChiDangHoc());      // C(3)
        assertEquals(13, progress.tinChiBatBuocYeuCau()); // A+B+C+E
        assertEquals(3, progress.tinChiBatBuocTichLuy()); // chỉ A, D là tự chọn
    }

    @Test
    void retakeUsesBestScoreAndPassBeatsInProgress() {
        TienDoHocTapDTO progress = TienDoCalculator.tinh(CHUONG_TRINH, Map.of(
                "A", lan(2.0, 7.5),           // học lại và đạt
                "B", lan(2.0, (Double) null), // từng rớt, đang học lại
                "C", lan(8.0, (Double) null)), // đã đạt dù có lần đăng ký khác chưa điểm
                4.0);
        List<MonChuongTrinhDTO> items = progress.chuongTrinh().monHoc();
        assertEquals(TrangThaiMonKhung.HOAN_THANH, items.get(0).trangThai());
        assertEquals(7.5, items.get(0).diemSo());
        assertEquals(TrangThaiMonKhung.DANG_HOC, items.get(1).trangThai());
        assertEquals(2.0, items.get(1).diemSo());
        assertEquals(TrangThaiMonKhung.HOAN_THANH, items.get(2).trangThai());
        assertEquals(6, progress.tinChiTichLuy());
        assertEquals(4, progress.tinChiDangHoc());
    }

    @Test
    void coursesOutsideProgramAreIgnoredAndEmptyProgramIsZero() {
        TienDoHocTapDTO outside = TienDoCalculator.tinh(CHUONG_TRINH, Map.of("ZZZ", lan(10.0)), 4.0);
        assertEquals(0, outside.tinChiTichLuy());

        TienDoHocTapDTO empty = TienDoCalculator.tinh(new ChuongTrinhKhungDTO("K", "Khoa", 0, List.of()),
                Map.of("A", lan(9.0)), 4.0);
        assertEquals(0, empty.tinChiYeuCau());
        assertEquals(0, empty.tinChiTichLuy());
        assertTrue(empty.chuongTrinh().monHoc().isEmpty());
    }
}
