package com.sms.server.security;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void expiresExactlyAfterSixtyIdleMinutes() throws Exception {
        MutableClock clock = new MutableClock();
        try (SessionManager sessions = new SessionManager(clock)) {
            String token = sessions.create("SV001", VaiTro.SINH_VIEN, "Nguyễn Minh Khang").token();
            clock.advance(Duration.ofMinutes(60));
            assertEquals(ErrorCode.CHUA_DANG_NHAP,
                    assertThrows(BusinessException.class, () -> sessions.require(token)).getCode());
        }
    }

    @Test
    void validActivityRenewsIdleTimeout() throws Exception {
        MutableClock clock = new MutableClock();
        try (SessionManager sessions = new SessionManager(clock)) {
            String token = sessions.create("SV001", VaiTro.SINH_VIEN, "Nguyễn Minh Khang").token();
            clock.advance(Duration.ofMinutes(59));
            assertEquals("SV001", sessions.require(token).tenDangNhap());
            clock.advance(Duration.ofMinutes(59));
            assertEquals("SV001", sessions.require(token).tenDangNhap());
        }
    }

    @Test
    void forbiddenRequestDoesNotRenewSession() throws Exception {
        MutableClock clock = new MutableClock();
        try (SessionManager sessions = new SessionManager(clock)) {
            String token = sessions.create("SV001", VaiTro.SINH_VIEN, "Nguyễn Minh Khang").token();
            clock.advance(Duration.ofMinutes(59));
            assertEquals(ErrorCode.KHONG_CO_QUYEN, assertThrows(BusinessException.class,
                    () -> sessions.require(token, VaiTro.QUAN_LY)).getCode());
            clock.advance(Duration.ofMinutes(1));
            assertEquals(ErrorCode.CHUA_DANG_NHAP, assertThrows(BusinessException.class,
                    () -> sessions.require(token)).getCode());
        }
    }

    @Test
    void revokesOtherSessionsAndPreservesCurrentUserSession() throws Exception {
        try (SessionManager sessions = new SessionManager()) {
            String keep = sessions.create("SV001", VaiTro.SINH_VIEN, "A").token();
            String revoked = sessions.create("SV001", VaiTro.SINH_VIEN, "A").token();
            String other = sessions.create("SV002", VaiTro.SINH_VIEN, "B").token();
            sessions.revokeUser("SV001", keep);
            assertNotNull(sessions.require(keep));
            assertNotNull(sessions.require(other));
            assertThrows(BusinessException.class, () -> sessions.require(revoked));
            sessions.remove(keep);
            assertThrows(BusinessException.class, () -> sessions.require(keep));
        }
    }

    @Test
    void rejectsNullAndUnknownTokens() {
        try (SessionManager sessions = new SessionManager()) {
            assertEquals(ErrorCode.CHUA_DANG_NHAP,
                    assertThrows(BusinessException.class, () -> sessions.require(null)).getCode());
            assertThrows(BusinessException.class, () -> sessions.require("unknown-token"));
        }
    }
}
