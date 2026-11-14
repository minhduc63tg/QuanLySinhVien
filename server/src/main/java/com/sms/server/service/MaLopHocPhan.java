package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.util.function.Predicate;

/**
 * Sinh mã lớp học phần = mã môn + số thứ tự 2 chữ số (ví dụ 420300362102, CTDL01).
 * Số thứ tự bắt đầu từ số lớp của môn đã mở trong học kỳ + 1; mã lớp là khóa chính toàn hệ thống
 * nên nếu mã đã dùng (ví dụ ở học kỳ trước) thì lấy số kế tiếp còn trống.
 */
final class MaLopHocPhan {

    static final int TOI_DA = 99;

    private MaLopHocPhan() {
    }

    static String tiepTheo(String maMon, long soLopDaMoTrongKy, Predicate<String> daTonTai) throws BusinessException {
        for (long n = soLopDaMoTrongKy + 1; n <= TOI_DA; n++) {
            String code = maMon + "%02d".formatted(n);
            if (!daTonTai.test(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE,
                "Môn " + maMon + " đã mở tối đa " + TOI_DA + " lớp, không thể sinh thêm mã lớp học phần");
    }
}
