package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichHocDTO;
import com.sms.common.enums.ErrorCode;
import java.util.List;

public final class LichHocRules {

    private LichHocRules() {
    }

    public static boolean overlaps(LichHocDTO a, LichHocDTO b) {
        return a.thu() == b.thu()
                && a.tietBatDau() <= b.tietBatDau() + b.soTiet() - 1
                && b.tietBatDau() <= a.tietBatDau() + a.soTiet() - 1;
    }

    public static void validate(List<LichHocDTO> schedule) throws BusinessException {
        ServiceSupport.check(schedule != null, "Lịch học không được null");
        for (int i = 0; i < schedule.size(); i++) {
            LichHocDTO slot = schedule.get(i);
            ServiceSupport.check(slot != null && slot.thu() >= 2 && slot.thu() <= 8
                    && slot.tietBatDau() >= 1 && slot.tietBatDau() <= 12
                    && slot.soTiet() >= 1 && slot.soTiet() <= 6
                    && slot.tietBatDau() + slot.soTiet() - 1 <= 12, "Thứ hoặc tiết học không hợp lệ");
            ServiceSupport.text(slot.phong(), 20, "Phòng học");
            for (int j = 0; j < i; j++) {
                if (overlaps(slot, schedule.get(j))) {
                    throw new BusinessException(ErrorCode.TRUNG_LICH, "Các dòng lịch của lớp bị trùng tiết");
                }
            }
        }
    }
}
