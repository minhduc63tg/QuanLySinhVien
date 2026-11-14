package com.sms.server.security;

import com.sms.common.BusinessException;
import com.sms.common.dto.SessionDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SessionManager implements AutoCloseable {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(60);
    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService cleaner;

    public SessionManager() {
        this(Clock.systemUTC());
    }

    SessionManager(Clock clock) {
        this.clock = clock;
        cleaner = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "sms-session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleAtFixedRate(this::removeExpired, 1, 1, TimeUnit.MINUTES);
    }

    public SessionDTO create(String username, VaiTro role, String fullName) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(username, role, fullName, clock.instant()));
        return new SessionDTO(token, username, role, fullName);
    }

    public SessionInfo require(String token, VaiTro... roles) throws BusinessException {
        if (token == null || token.isBlank()) {
            throw unauthenticated();
        }
        Instant now = clock.instant();
        SessionInfo info = sessions.computeIfPresent(token, (key, current) -> {
            if (expired(current, now)) {
                return null;
            }
            return allowed(current, roles)
                    ? new SessionInfo(current.tenDangNhap(), current.vaiTro(), current.hoTen(), now)
                    : current;
        });
        if (info == null) {
            throw unauthenticated();
        }
        if (!allowed(info, roles)) {
            throw new BusinessException(ErrorCode.KHONG_CO_QUYEN, "Bạn không có quyền thực hiện thao tác này");
        }
        return info;
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void revokeUser(String username, String tokenToKeep) {
        sessions.entrySet().removeIf(entry -> entry.getValue().tenDangNhap().equals(username)
                && !entry.getKey().equals(tokenToKeep));
    }

    private void removeExpired() {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> expired(entry.getValue(), now));
    }

    private static boolean allowed(SessionInfo info, VaiTro[] roles) {
        return roles.length == 0 || Arrays.asList(roles).contains(info.vaiTro());
    }

    private static boolean expired(SessionInfo info, Instant now) {
        return !now.isBefore(info.lastActivity().plus(IDLE_TIMEOUT));
    }

    private static BusinessException unauthenticated() {
        return new BusinessException(ErrorCode.CHUA_DANG_NHAP, "Phiên đã hết hạn, vui lòng đăng nhập lại");
    }

    @Override
    public void close() {
        cleaner.shutdownNow();
        sessions.clear();
    }
}
