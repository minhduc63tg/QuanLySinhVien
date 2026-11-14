package com.sms.server.security;

import java.nio.charset.StandardCharsets;
import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static boolean validLength(String password) {
        return password != null && !password.isBlank()
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public static String hash(String password) {
        if (!validLength(password)) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 1 đến 72 byte UTF-8.");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean matches(String password, String hash) {
        return validLength(password) && hash != null && BCrypt.checkpw(password, hash);
    }
}
