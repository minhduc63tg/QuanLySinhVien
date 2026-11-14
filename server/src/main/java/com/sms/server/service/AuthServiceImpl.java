package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.SessionDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.AuthService;
import com.sms.server.entity.GiangVien;
import com.sms.server.entity.SinhVien;
import com.sms.server.entity.TaiKhoan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.LoginAttemptLimiter;
import com.sms.server.security.PasswordUtil;
import com.sms.server.security.SessionInfo;
import com.sms.server.security.SessionManager;
import jakarta.persistence.LockModeType;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class AuthServiceImpl extends ServiceSupport implements AuthService {

    private static final String DUMMY_HASH = PasswordUtil.hash("sms-dummy-password");
    private final LoginAttemptLimiter limiter;
    private final ReentrantReadWriteLock credentials = new ReentrantReadWriteLock();

    public AuthServiceImpl(JpaUtil jpa, SessionManager sessions) {
        this(jpa, sessions, new LoginAttemptLimiter());
    }

    public AuthServiceImpl(JpaUtil jpa, SessionManager sessions, LoginAttemptLimiter limiter) {
        super(jpa, sessions);
        this.limiter = limiter;
    }

    @Override
    public SessionDTO dangNhap(String tenDangNhap, String matKhau) throws BusinessException {
        credentials.readLock().lock();
        try {
            return login(tenDangNhap, matKhau);
        } finally {
            credentials.readLock().unlock();
        }
    }

    private SessionDTO login(String tenDangNhap, String matKhau) throws BusinessException {
        String username = text(tenDangNhap, 20, "Tên đăng nhập");
        LoginResult result = transaction(em -> {
            TaiKhoan account = em.find(TaiKhoan.class, username);
            // Dùng tên thật trong DB làm khóa để các cách viết hoa/thường không vượt được giới hạn.
            String key = account == null ? username.toLowerCase(Locale.ROOT) : account.getTenDangNhap();
            return limiter.authenticate(key, () -> {
                boolean matches = PasswordUtil.matches(matKhau,
                        account == null ? DUMMY_HASH : account.getMatKhau());
                if (!matches || account == null) {
                    return null;
                }
                String fullName = account.getTenDangNhap();
                if (account.getVaiTro() == VaiTro.SINH_VIEN) {
                    fullName = find(em, SinhVien.class, account.getTenDangNhap()).getHoTen();
                } else if (account.getVaiTro() == VaiTro.GIANG_VIEN) {
                    fullName = find(em, GiangVien.class, account.getTenDangNhap()).getHoTen();
                }
                return new LoginResult(account.getTenDangNhap(), account.getVaiTro(), fullName);
            });
        });
        return sessions.create(result.username(), result.role(), result.fullName());
    }

    @Override
    public void dangXuat(String token) throws BusinessException {
        sessions.require(token);
        sessions.remove(token);
    }

    @Override
    public void doiMatKhau(String token, String matKhauCu, String matKhauMoi) throws BusinessException {
        credentials.writeLock().lock();
        try {
            changePassword(token, matKhauCu, matKhauMoi);
        } finally {
            credentials.writeLock().unlock();
        }
    }

    private void changePassword(String token, String matKhauCu, String matKhauMoi) throws BusinessException {
        SessionInfo info = sessions.require(token);
        check(PasswordUtil.validLength(matKhauMoi) && matKhauMoi.length() >= 6,
                "Mật khẩu mới phải có ít nhất 6 ký tự và không quá 72 byte UTF-8");
        transaction(em -> {
            TaiKhoan account = em.find(TaiKhoan.class, info.tenDangNhap(), LockModeType.PESSIMISTIC_WRITE);
            if (account == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy tài khoản");
            }
            if (!PasswordUtil.matches(matKhauCu, account.getMatKhau())) {
                throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP, "Mật khẩu hiện tại không đúng");
            }
            account.setMatKhau(PasswordUtil.hash(matKhauMoi));
            return null;
        });
        sessions.revokeUser(info.tenDangNhap(), token);
    }

    @Override
    public void resetMatKhau(String token, String tenDangNhap) throws BusinessException {
        credentials.writeLock().lock();
        try {
            resetPassword(token, tenDangNhap);
        } finally {
            credentials.writeLock().unlock();
        }
    }

    private void resetPassword(String token, String tenDangNhap) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String username = text(tenDangNhap, 20, "Tên đăng nhập");
        String canonical = transaction(em -> {
            TaiKhoan account = em.find(TaiKhoan.class, username, LockModeType.PESSIMISTIC_WRITE);
            if (account == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy tài khoản");
            }
            account.setMatKhau(PasswordUtil.hash(account.getTenDangNhap()));
            return account.getTenDangNhap();
        });
        sessions.revokeUser(canonical, null);
    }

    private record LoginResult(String username, VaiTro role, String fullName) {
    }
}
