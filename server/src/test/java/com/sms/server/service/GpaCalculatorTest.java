package com.sms.server.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class GpaCalculatorTest {

    @ParameterizedTest
    @CsvSource({
            "0.0,F,0.0,false", "3.9,F,0.0,false", "4.0,D,1.0,true", "4.9,D,1.0,true",
            "5.0,D+,1.5,true", "5.4,D+,1.5,true", "5.5,C,2.0,true", "5.9,C,2.0,true",
            "6.0,C+,2.5,true", "6.9,C+,2.5,true", "7.0,B,3.0,true", "7.9,B,3.0,true",
            "8.0,B+,3.5,true", "8.4,B+,3.5,true", "8.5,A,3.8,true", "8.9,A,3.8,true",
            "9.0,A+,4.0,true", "10.0,A+,4.0,true"
    })
    void convertsEveryBoundary(double score, String letter, double points, boolean passed) {
        assertEquals(new GpaCalculator.KetQuaQuyDoi(letter, points, passed), GpaCalculator.quyDoi(score));
    }

    @Test
    void usesBestAttemptWeightsCreditsAndIncludesFailedCourses() {
        assertEquals((4.0 * 3 + 0.0 * 2) / 5, GpaCalculator.tinhGpa(List.of(
                new GpaCalculator.DiemMon("A", 3, 4.0),
                new GpaCalculator.DiemMon("A", 3, 9.0),
                new GpaCalculator.DiemMon("B", 2, 3.9),
                new GpaCalculator.DiemMon("C", 4, null))), 1e-10);
    }

    @Test
    void noGradesMeansNoGpa() {
        assertNull(GpaCalculator.tinhGpa(List.of()));
        assertNull(GpaCalculator.tinhGpa(List.of(new GpaCalculator.DiemMon("A", 3, null))));
    }

    @Test
    void roundsHalfUpAndRejectsInvalidGrades() {
        assertEquals(8.6, GpaCalculator.lamTron(8.55));
        for (double invalid : new double[] {-0.1, 10.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> GpaCalculator.quyDoi(invalid));
            assertThrows(IllegalArgumentException.class, () -> GpaCalculator.lamTron(invalid));
        }
    }
}
