package com.sms.server.security;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginAttemptLimiter {

    private static final Duration LOCK_TIME = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Object[] locks = new Object[64];
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    @FunctionalInterface
    public interface LoginWork<T> {
        T run() throws BusinessException;
    }

    public <T> T authenticate(String username, LoginWork<T> work) throws BusinessException {
        synchronized (locks[Math.floorMod(username.hashCode(), locks.length)]) {
            Instant now = clock.instant();
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            Attempt attempt = attempts.get(username);
            if (attempt != null && attempt.failures() >= 5) {
                throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP,
                        "Đăng nhập sai nhiều lần, vui lòng thử lại sau 5 phút");
            }
            T result = work.run();
            if (result != null) {
                attempts.remove(username);
                return result;
            }
            int failures = attempt == null ? 1 : attempt.failures() + 1;
            attempts.put(username, new Attempt(failures, clock.instant().plus(LOCK_TIME)));
            throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP, "Sai thông tin đăng nhập");
        }
    }

    private record Attempt(int failures, Instant expiresAt) {
    }
}
