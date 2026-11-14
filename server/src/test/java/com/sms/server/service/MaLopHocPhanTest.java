package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaLopHocPhanTest {

    @Test
    void joinsCourseCodeWithTwoDigitSequence() throws Exception {
        assertEquals("42030036210201", MaLopHocPhan.tiepTheo("420300362102", 0, code -> false));
        assertEquals("CTDL01", MaLopHocPhan.tiepTheo("CTDL", 0, code -> false));
        assertEquals("CTDL03", MaLopHocPhan.tiepTheo("CTDL", 2, code -> false));
        assertEquals("CTDL10", MaLopHocPhan.tiepTheo("CTDL", 9, code -> false));
    }

    @Test
    void skipsCodesAlreadyUsedByOtherSemesters() throws Exception {
        Set<String> used = new HashSet<>(Set.of("CTDL01", "CTDL02", "CTDL04"));
        // Học kỳ mới chưa mở lớp nào: bắt đầu từ 01 nhưng 01, 02 đã dùng nên lấy 03
        assertEquals("CTDL03", MaLopHocPhan.tiepTheo("CTDL", 0, used::contains));
        // Đã mở 2 lớp trong kỳ: thử 03 rồi
        assertEquals("CTDL03", MaLopHocPhan.tiepTheo("CTDL", 2, used::contains));
        // 04 đã dùng thì nhảy sang 05
        assertEquals("CTDL05", MaLopHocPhan.tiepTheo("CTDL", 3, used::contains));
    }

    @Test
    void failsWhenAllNinetyNineNumbersAreTaken() {
        assertEquals("CTDL99", assertDoesNotThrow(() -> MaLopHocPhan.tiepTheo("CTDL", 98, code -> false)));
        // Đã mở 99 lớp: không còn số
        BusinessException full = assertThrows(BusinessException.class,
                () -> MaLopHocPhan.tiepTheo("CTDL", 99, code -> false));
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, full.getCode());
        // Mọi số đều đã bị dùng ở học kỳ khác
        BusinessException taken = assertThrows(BusinessException.class,
                () -> MaLopHocPhan.tiepTheo("CTDL", 0, code -> true));
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, taken.getCode());
    }
}
