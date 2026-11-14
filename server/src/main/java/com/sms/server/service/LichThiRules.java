package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichThiDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Quy tắc thuần cho lịch thi (có unit test): kiểm tra dữ liệu và phát hiện trùng giờ. */
public final class LichThiRules {

    public static final int TOI_DA_GIAM_THI = 5;

    private LichThiRules() {
    }

    /** Hai buổi thi trùng giờ khi cùng ngày và hai đoạn tiết giao nhau. */
    public static boolean overlaps(LocalDate dayA, int startA, int countA,
            LocalDate dayB, int startB, int countB) {
        return dayA.equals(dayB) && startA <= startB + countB - 1 && startB <= startA + countA - 1;
    }

    /** Kiểm tra dữ liệu gửi lên và trả về danh sách mã giám thị đã chuẩn hóa (bỏ khoảng trắng, không trùng). */
    public static List<String> validate(LichThiDTO dto) throws BusinessException {
        ServiceSupport.check(dto != null, "Lịch thi không được null");
        ServiceSupport.check(dto.ngayThi() != null, "Ngày thi không được trống");
        ServiceSupport.check(dto.tietBatDau() >= 1 && dto.tietBatDau() <= 12
                && dto.soTiet() >= 1 && dto.soTiet() <= 6
                && dto.tietBatDau() + dto.soTiet() - 1 <= 12, "Tiết thi không hợp lệ (tiết 1-12, tối đa 6 tiết)");
        ServiceSupport.text(dto.phong(), 20, "Phòng thi");
        List<String> codes = new ArrayList<>();
        if (dto.maGiamThi() != null) {
            for (String code : dto.maGiamThi()) {
                codes.add(ServiceSupport.text(code, 20, "Mã giám thị"));
            }
        }
        ServiceSupport.check(new HashSet<>(codes).size() == codes.size(), "Giám thị bị khai báo trùng");
        ServiceSupport.check(codes.size() <= TOI_DA_GIAM_THI,
                "Mỗi buổi thi có tối đa " + TOI_DA_GIAM_THI + " giám thị");
        return codes;
    }

    static Set<String> intersection(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }
}
