package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

@Slf4j
@RequiredArgsConstructor
abstract class ServiceSupport {

    protected final JpaUtil jpa;
    protected final SessionManager sessions;

    @FunctionalInterface
    protected interface Work<T> {
        T apply(EntityManager em) throws BusinessException;
    }

    protected final <T> T transaction(Work<T> work) throws BusinessException {
        try {
            return jpa.inTransaction(em -> {
                try {
                    return work.apply(em);
                } catch (BusinessException error) {
                    throw new BusinessFailure(error);
                }
            });
        } catch (BusinessFailure failure) {
            throw failure.error;
        } catch (RuntimeException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof jakarta.persistence.OptimisticLockException
                        || cause instanceof org.hibernate.StaleStateException) {
                    throw new BusinessException(ErrorCode.XUNG_DOT_DU_LIEU, "Dữ liệu vừa thay đổi, vui lòng thử lại");
                }
                // MariaDB: 1213 = deadlock, 1020 = "Record has changed since last read",
                // SQLState 40001 = serialization failure. Đều là xung đột giao dịch, thử lại được.
                if (cause instanceof java.sql.SQLException sql
                        && (sql.getErrorCode() == 1213 || sql.getErrorCode() == 1020
                                || "40001".equals(sql.getSQLState()))) {
                    throw new BusinessException(ErrorCode.XUNG_DOT_DU_LIEU, "Dữ liệu vừa thay đổi, vui lòng thử lại");
                }
                if (cause instanceof ConstraintViolationException constraint) {
                    int code = constraint.getSQLException().getErrorCode();
                    if (code == 1062) {
                        throw new BusinessException(ErrorCode.DA_TON_TAI, "Dữ liệu đã tồn tại");
                    }
                    if (code == 1451 || code == 1452) {
                        throw new BusinessException(ErrorCode.RANG_BUOC, "Dữ liệu đang có liên kết, không thể thực hiện thao tác");
                    }
                }
            }
            log.error("Lỗi khi xử lý service", failure);
            throw new BusinessException(ErrorCode.LOI_HE_THONG, "Lỗi hệ thống, vui lòng thử lại");
        }
    }

    protected final <T> T retryTransaction(int maxAttempts, Work<T> work) throws BusinessException {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return transaction(work);
            } catch (BusinessException error) {
                if (error.getCode() != ErrorCode.XUNG_DOT_DU_LIEU || attempt == maxAttempts) {
                    throw error;
                }
            }
        }
        throw new IllegalArgumentException("Số lần thử phải lớn hơn 0");
    }

    protected static void check(boolean valid, String message) throws BusinessException {
        if (!valid) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, message);
        }
    }

    protected static String text(String value, int max, String label) throws BusinessException {
        check(value != null && !value.isBlank() && value.trim().length() <= max,
                label + " không được trống và không quá " + max + " ký tự");
        return value.trim();
    }

    protected static <T> T find(EntityManager em, Class<T> type, Object key) throws BusinessException {
        T value = em.find(type, key);
        if (value == null) {
            throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy dữ liệu");
        }
        return value;
    }

    protected static void noDependency(long count) throws BusinessException {
        if (count > 0) {
            throw new BusinessException(ErrorCode.RANG_BUOC, "Dữ liệu đang được sử dụng, không thể xóa");
        }
    }

    private static final class BusinessFailure extends RuntimeException {
        private final BusinessException error;

        private BusinessFailure(BusinessException error) {
            super(error);
            this.error = error;
        }
    }
}
