package com.sms.common.enums;

/** Trạng thái của một môn trong chương trình khung đối với một sinh viên. */
public enum TrangThaiMonKhung {
    /** Đã có điểm đạt (&gt;= điểm qua môn). */
    HOAN_THANH,
    /** Đang đăng ký, chưa có điểm. */
    DANG_HOC,
    /** Đã có điểm nhưng chưa đạt. */
    CHUA_DAT,
    /** Chưa đăng ký lần nào. */
    CHUA_HOC
}
