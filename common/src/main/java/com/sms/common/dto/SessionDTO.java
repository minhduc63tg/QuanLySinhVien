package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import com.sms.common.enums.VaiTro;

/** Phiên do server cấp sau khi đăng nhập; không chứa mật khẩu. */
public record SessionDTO(String token, String tenDangNhap, VaiTro vaiTro, String hoTen) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

