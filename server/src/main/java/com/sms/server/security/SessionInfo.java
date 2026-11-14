package com.sms.server.security;

import com.sms.common.enums.VaiTro;
import java.time.Instant;

public record SessionInfo(String tenDangNhap, VaiTro vaiTro, String hoTen, Instant lastActivity) {
}
