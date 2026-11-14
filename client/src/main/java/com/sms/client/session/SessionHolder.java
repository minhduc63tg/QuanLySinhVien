package com.sms.client.session;

import com.sms.common.dto.SessionDTO;

/** Phiên đăng nhập hiện tại của client (một người dùng mỗi tiến trình). */
public final class SessionHolder {

    private static volatile SessionDTO session;

    private SessionHolder() {
    }

    public static void set(SessionDTO value) {
        session = value;
    }

    public static SessionDTO get() {
        return session;
    }

    public static String token() {
        SessionDTO current = session;
        return current == null ? null : current.token();
    }

    public static boolean isLoggedIn() {
        return session != null;
    }

    public static void clear() {
        session = null;
    }
}
