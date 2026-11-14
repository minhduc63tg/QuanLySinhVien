package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.*;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.TrangThaiHocTap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DangKyRulesTest {

    private static final String SEMESTER = "HK1-2026-2027";

    @Test
    void acceptsPassedPrerequisitesFromPreviousSemesterAndExactCreditLimit() {
        assertDoesNotThrow(() -> validate(section("NEW", "ADV", SEMESTER, 3, 0, 2), Set.of("BASE"),
                List.of(registration(section("OLD", "BASE", "HK2-2025-2026", 3, 0, 2), 4.0),
                        registration(section("OTHER", "OTHER", SEMESTER, 3, 0, 3), null))));
    }

    @Test
    void rejectsMissingOrFailedPrerequisite() {
        assertError(ErrorCode.THIEU_MON_TIEN_QUYET,
                () -> validate(section("N", "ADV", SEMESTER, 3, 0, 2), Set.of("BASE"), List.of()));
        assertError(ErrorCode.THIEU_MON_TIEN_QUYET,
                () -> validate(section("N", "ADV", SEMESTER, 3, 0, 2), Set.of("BASE"),
                        List.of(registration(section("OLD", "BASE", "OLD", 3, 0, 2), 3.9))));
    }

    @Test
    void rejectsScheduleOverlap() {
        assertError(ErrorCode.TRUNG_LICH, () -> validate(section("N", "NEW", SEMESTER, 3, 0, 2),
                Set.of(), List.of(registration(section("OLD", "OTHER", SEMESTER, 3, 0, 2), null))));
    }

    @Test
    void rejectsExcessiveCredits() {
        assertError(ErrorCode.VUOT_TIN_CHI, () -> validate(section("N", "NEW", SEMESTER, 4, 0, 3),
                Set.of(), List.of(registration(section("OLD", "OTHER", SEMESTER, 3, 0, 2), null))));
    }

    @Test
    void rejectsFullClass() {
        assertError(ErrorCode.LOP_DAY,
                () -> validate(section("N", "NEW", SEMESTER, 3, 1, 2), Set.of(), List.of()));
    }

    @Test
    void rejectsDuplicateSectionAndAnotherSectionOfSameCourse() {
        var target = section("N", "COURSE", SEMESTER, 3, 0, 2);
        assertError(ErrorCode.DA_DANG_KY,
                () -> validate(target, Set.of(), List.of(registration(target, null))));
        assertError(ErrorCode.DA_DANG_KY, () -> validate(target, Set.of(),
                List.of(registration(section("OTHER", "COURSE", SEMESTER, 3, 0, 3), null))));
    }

    @Test
    void rejectsGraduatedStudentAndClosedSemester() {
        var target = section("N", "NEW", SEMESTER, 3, 0, 2);
        assertError(ErrorCode.SINH_VIEN_TOT_NGHIEP, () -> DangKyRules.validate(
                TrangThaiHocTap.TOT_NGHIEP, SEMESTER, 6, 4, target, Set.of(), List.of()));
        assertError(ErrorCode.NGOAI_HOC_KY_DANG_KY,
                () -> validate(section("N", "NEW", "OLD", 3, 0, 2), Set.of(), List.of()));
    }

    private static void validate(LopHocPhanDTO target, Set<String> prerequisites, List<DangKyDTO> history)
            throws BusinessException {
        DangKyRules.validate(TrangThaiHocTap.DANG_HOC, SEMESTER, 6, 4, target, prerequisites, history);
    }

    private static LopHocPhanDTO section(String id, String course, String semester, int credits, int count, int day) {
        return new LopHocPhanDTO(id, course, course, credits, null, null, semester, 1, count, 0,
                List.of(new LichHocDTO(null, day, 1, 3, "A101")));
    }

    private static DangKyDTO registration(LopHocPhanDTO section, Double grade) {
        return new DangKyDTO(section, LocalDateTime.of(2026, 11, 12, 9, 0), grade);
    }

    @FunctionalInterface
    private interface CheckedCall { void run() throws BusinessException; }

    private static void assertError(ErrorCode code, CheckedCall work) {
        assertEquals(code, assertThrows(BusinessException.class, work::run).getCode());
    }
}
