package com.sms.server.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashesWithSaltAndChecksCorrectPassword() {
        String first = PasswordUtil.hash("MậtKhẩu@123");
        String second = PasswordUtil.hash("MậtKhẩu@123");
        assertNotEquals(first, second);
        assertEquals(60, first.length());
        assertTrue(PasswordUtil.matches("MậtKhẩu@123", first));
        assertFalse(PasswordUtil.matches("wrong", first));
        assertFalse(PasswordUtil.matches(null, first));
    }

    @Test
    void enforcesBcryptByteLimitWithoutTruncatingUnicode() {
        assertTrue(PasswordUtil.validLength("a".repeat(72)));
        assertFalse(PasswordUtil.validLength("a".repeat(73)));
        assertFalse(PasswordUtil.validLength("ệ".repeat(25)));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("ệ".repeat(25)));
    }
}
