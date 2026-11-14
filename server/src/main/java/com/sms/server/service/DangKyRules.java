package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.DangKyDTO;
import com.sms.common.dto.LopHocPhanDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.TrangThaiHocTap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DangKyRules {

    private DangKyRules() {
    }

    public static void validate(TrangThaiHocTap status, String semester, int maxCredits,
            double passGrade, LopHocPhanDTO target, Set<String> prerequisites,
            List<DangKyDTO> history) throws BusinessException {
        require(status != TrangThaiHocTap.TOT_NGHIEP, ErrorCode.SINH_VIEN_TOT_NGHIEP,
                "Sinh viên đã tốt nghiệp không được đăng ký học phần");
        require(target.hocKy().equals(semester), ErrorCode.NGOAI_HOC_KY_DANG_KY,
                "Lớp không thuộc học kỳ đang mở đăng ký");
        List<DangKyDTO> current = history.stream()
                .filter(d -> d.lopHocPhan().hocKy().equals(semester)).toList();
        require(current.stream().noneMatch(d -> d.lopHocPhan().maLhp().equals(target.maLhp())),
                ErrorCode.DA_DANG_KY, "Bạn đã đăng ký lớp học phần này");
        require(current.stream().noneMatch(d -> d.lopHocPhan().maMon().equals(target.maMon())),
                ErrorCode.DA_DANG_KY, "Bạn đã đăng ký lớp khác của cùng môn trong học kỳ");
        Set<String> passed = new HashSet<>();
        history.stream().filter(d -> d.diemSo() != null && d.diemSo() >= passGrade)
                .forEach(d -> passed.add(d.lopHocPhan().maMon()));
        require(passed.containsAll(prerequisites), ErrorCode.THIEU_MON_TIEN_QUYET,
                "Bạn chưa đạt đủ các môn tiên quyết");
        for (DangKyDTO registration : current) {
            for (var oldSlot : registration.lopHocPhan().lichHoc()) {
                for (var newSlot : target.lichHoc()) {
                    require(!LichHocRules.overlaps(oldSlot, newSlot), ErrorCode.TRUNG_LICH,
                            "Lớp học phần trùng lịch với lớp đã đăng ký");
                }
            }
        }
        int credits = current.stream().mapToInt(d -> d.lopHocPhan().soTinChi()).sum();
        require(credits + target.soTinChi() <= maxCredits, ErrorCode.VUOT_TIN_CHI,
                "Tổng tín chỉ vượt giới hạn học kỳ");
        require(target.soLuongDaDk() < target.soLuongToiDa(), ErrorCode.LOP_DAY,
                "Lớp học phần đã đủ sĩ số");
    }

    private static void require(boolean condition, ErrorCode code, String message) throws BusinessException {
        if (!condition) throw new BusinessException(code, message);
    }
}
