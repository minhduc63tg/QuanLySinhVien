package com.sms.common;

import com.sms.common.enums.ErrorCode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Lỗi nghiệp vụ có thông điệp tiếng Việt để hiển thị trực tiếp cho người dùng. */
public class BusinessException extends Exception implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode getCode() {
        return code;
    }
}

