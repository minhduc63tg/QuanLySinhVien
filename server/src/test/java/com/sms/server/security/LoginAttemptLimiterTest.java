package com.sms.server.security;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void locksAfterFiveFailuresAndUnlocksAfterFiveMinutes() throws Exception {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        for (int i = 0; i < 5; i++) {
            assertEquals(ErrorCode.SAI_THONG_TIN_DANG_NHAP, assertThrows(BusinessException.class,
                    () -> limiter.authenticate("admin", () -> null)).getCode());
        }
        AtomicBoolean called = new AtomicBoolean();
        assertThrows(BusinessException.class, () -> limiter.authenticate("admin", () -> {
            called.set(true);
            return "ok";
        }));
        assertFalse(called.get());
        assertEquals("other", limiter.authenticate("another-user", () -> "other"));
        clock.advance(Duration.ofMinutes(5));
        assertEquals("ok", limiter.authenticate("admin", () -> "ok"));
    }

    @Test
    void successfulLoginResetsConsecutiveFailures() throws Exception {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) {
            assertThrows(BusinessException.class, () -> limiter.authenticate("admin", () -> null));
        }
        assertEquals("ok", limiter.authenticate("admin", () -> "ok"));
        assertThrows(BusinessException.class, () -> limiter.authenticate("admin", () -> null));
        assertEquals("ok", limiter.authenticate("admin", () -> "ok"));
    }
}
