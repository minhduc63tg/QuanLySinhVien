package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.server.entity.TaiKhoan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.PasswordUtil;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;

abstract class HoSoSupport extends ServiceSupport {

    HoSoSupport(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    static String optional(String value, int max, String label) throws BusinessException {
        return value == null || value.isBlank() ? null : text(value, max, label);
    }

    static String email(String value) throws BusinessException {
        String result = optional(value, 100, "Email");
        check(result == null || result.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Email không hợp lệ");
        return result;
    }

    static String phone(String value) throws BusinessException {
        String result = optional(value, 15, "Số điện thoại");
        check(result == null || result.matches("[+0-9][0-9 .()-]*"), "Số điện thoại không hợp lệ");
        return result;
    }

    static TaiKhoan createAccount(EntityManager em, String code, VaiTro role) throws BusinessException {
        if (em.find(TaiKhoan.class, code) != null) {
            throw new BusinessException(ErrorCode.DA_TON_TAI, "Mã người dùng đã tồn tại");
        }
        TaiKhoan account = new TaiKhoan();
        account.setTenDangNhap(code);
        account.setMatKhau(PasswordUtil.hash(code));
        account.setVaiTro(role);
        em.persist(account);
        return account;
    }
}
