package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichHocDTO;
import com.sms.common.enums.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LichHocRulesTest {

    @Test
    void overlapIncludesSharedEndpointButNotAdjacentPeriodsOrDifferentDays() {
        var first = new LichHocDTO(null, 2, 1, 3, "A101");
        assertTrue(LichHocRules.overlaps(first, new LichHocDTO(null, 2, 3, 2, "B101")));
        assertFalse(LichHocRules.overlaps(first, new LichHocDTO(null, 2, 4, 2, "A101")));
        assertFalse(LichHocRules.overlaps(first, new LichHocDTO(null, 3, 1, 3, "A101")));
    }

    @Test
    void validatesSundayAndLastPeriod() {
        assertDoesNotThrow(() -> LichHocRules.validate(List.of(new LichHocDTO(null, 8, 12, 1, "A101"))));
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, assertThrows(BusinessException.class,
                () -> LichHocRules.validate(List.of(new LichHocDTO(null, 8, 12, 2, "A101")))).getCode());
    }

    @Test
    void rejectsOverlappingRowsWithinOneSection() {
        assertEquals(ErrorCode.TRUNG_LICH, assertThrows(BusinessException.class,
                () -> LichHocRules.validate(List.of(
                        new LichHocDTO(null, 2, 1, 3, "A101"),
                        new LichHocDTO(null, 2, 3, 3, "B101")))).getCode());
    }
}
