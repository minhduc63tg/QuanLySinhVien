package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TienQuyetValidatorTest {

    private final Map<String, Set<String>> graph = Map.of(
            "LTCB", Set.of(), "OOP", Set.of("LTCB"),
            "CTDL", Set.of("OOP"), "LTM", Set.of("CTDL"));

    @Test
    void rejectsSelfReference() {
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, assertThrows(BusinessException.class,
                () -> TienQuyetValidator.validate("OOP", Set.of("OOP"), graph)).getCode());
    }

    @Test
    void rejectsIndirectCycle() {
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, assertThrows(BusinessException.class,
                () -> TienQuyetValidator.validate("LTCB", Set.of("LTM"), graph)).getCode());
    }

    @Test
    void acceptsValidDependenciesAndRemoval() {
        assertDoesNotThrow(() -> TienQuyetValidator.validate("LTM", Set.of("CTDL", "LTCB"), graph));
        assertDoesNotThrow(() -> TienQuyetValidator.validate("OOP", Set.of(), graph));
    }

    @Test
    void rejectsUnknownCourse() {
        assertEquals(ErrorCode.KHONG_TIM_THAY, assertThrows(BusinessException.class,
                () -> TienQuyetValidator.validate("OOP", Set.of("UNKNOWN"), graph)).getCode());
    }
}
