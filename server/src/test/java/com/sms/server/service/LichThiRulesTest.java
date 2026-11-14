package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichThiDTO;
import com.sms.common.enums.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LichThiRulesTest {

    private static final LocalDate NGAY = LocalDate.of(2027, 1, 11);

    private static LichThiDTO thi(LocalDate ngay, int tiet, int soTiet, String phong, List<String> giamThi) {
        return new LichThiDTO("L1", null, null, null, ngay, tiet, soTiet, phong, giamThi, null, 0);
    }

    @Test
    void overlapNeedsSameDayAndIntersectingPeriods() {
        assertTrue(LichThiRules.overlaps(NGAY, 1, 3, NGAY, 3, 2));      // tiết 3 chung
        assertTrue(LichThiRules.overlaps(NGAY, 3, 2, NGAY, 1, 3));
        assertFalse(LichThiRules.overlaps(NGAY, 1, 3, NGAY, 4, 2));     // liền kề nhưng không giao
        assertFalse(LichThiRules.overlaps(NGAY, 1, 3, NGAY.plusDays(1), 1, 3)); // khác ngày
        assertTrue(LichThiRules.overlaps(NGAY, 5, 1, NGAY, 5, 1));
    }

    @Test
    void acceptsValidExamAndTrimsInvigilators() throws Exception {
        assertEquals(List.of("GV001", "GV002"),
                LichThiRules.validate(thi(NGAY, 11, 2, "A101", List.of(" GV001 ", "GV002"))));
        assertEquals(List.of(), LichThiRules.validate(thi(NGAY, 1, 1, "A101", null)));
        assertDoesNotThrow(() -> LichThiRules.validate(thi(NGAY, 7, 6, "A101", List.of())));
    }

    @Test
    void rejectsInvalidData() {
        for (LichThiDTO bad : List.of(
                thi(null, 1, 3, "A101", List.of()),                  // thiếu ngày
                thi(NGAY, 0, 3, "A101", List.of()),                  // tiết 0
                thi(NGAY, 13, 1, "A101", List.of()),                 // tiết 13
                thi(NGAY, 1, 7, "A101", List.of()),                  // quá 6 tiết
                thi(NGAY, 11, 3, "A101", List.of()),                 // vượt tiết 12
                thi(NGAY, 1, 3, " ", List.of()),                     // phòng trống
                thi(NGAY, 1, 3, "A101", List.of("G1", "G1")),        // giám thị trùng
                thi(NGAY, 1, 3, "A101", List.of("1", "2", "3", "4", "5", "6")))) { // quá 5 giám thị
            BusinessException error = assertThrows(BusinessException.class, () -> LichThiRules.validate(bad));
            assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, error.getCode());
        }
        assertThrows(BusinessException.class, () -> LichThiRules.validate(null));
    }
}
