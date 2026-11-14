package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Tiến độ học tập so với chương trình khung của khoa. Tín chỉ tích lũy chỉ tính các môn thuộc
 * chương trình khung đã đạt; tinChiYeuCau = 0 nghĩa là khoa chưa có chương trình khung.
 */
public record TienDoHocTapDTO(ChuongTrinhKhungDTO chuongTrinh, int tinChiYeuCau, int tinChiTichLuy,
        int tinChiDangHoc, int tinChiBatBuocYeuCau, int tinChiBatBuocTichLuy) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
