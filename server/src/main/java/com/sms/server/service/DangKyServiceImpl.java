package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.*;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.DangKyService;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.HocTapMapper;
import com.sms.server.security.SessionManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public final class DangKyServiceImpl extends ServiceSupport implements DangKyService {

    private final AppConfig config;

    public DangKyServiceImpl(JpaUtil jpa, SessionManager sessions, AppConfig config) {
        super(jpa, sessions);
        this.config = config;
    }

    @Override
    public String layHocKyDangKy(String token) throws BusinessException {
        sessions.require(token, VaiTro.SINH_VIEN);
        return config.hocKyDangKy();
    }

    @Override
    public void dangKy(String token, String maLhp) throws BusinessException {
        sessions.require(token, VaiTro.SINH_VIEN);
        String code = text(maLhp, 30, "Mã lớp học phần");
        try {
            retryTransaction(config.soLanThuLai(), em -> {
                var session = sessions.require(token, VaiTro.SINH_VIEN);
                SinhVien sv = em.find(SinhVien.class, session.tenDangNhap(), LockModeType.PESSIMISTIC_WRITE);
                if (sv == null) sv = find(em, SinhVien.class, session.tenDangNhap());
                if (sv.getTrangThai() == com.sms.common.enums.TrangThaiHocTap.TOT_NGHIEP) {
                    throw new BusinessException(ErrorCode.SINH_VIEN_TOT_NGHIEP,
                            "Sinh viên đã tốt nghiệp không được đăng ký học phần");
                }
                LopHocPhan lhp = find(em, LopHocPhan.class, code);
                List<DangKyDTO> history = HocTapQueries.history(em, sv.getMaSv()).stream()
                        .map(HocTapMapper::dangKy).toList();
                validate(sv, lhp, history);
                DangKyHocPhan registration = new DangKyHocPhan();
                registration.setId(new DangKyId(sv.getMaSv(), lhp.getMaLhp()));
                registration.setSinhVien(sv);
                registration.setLopHocPhan(lhp);
                registration.setNgayDangKy(LocalDateTime.now());
                lhp.setSoLuongDaDk(lhp.getSoLuongDaDk() + 1);
                em.persist(registration);
                return null;
            });
        } catch (BusinessException error) {
            if (error.getCode() == ErrorCode.DA_TON_TAI) {
                throw new BusinessException(ErrorCode.DA_DANG_KY, "Bạn đã đăng ký lớp học phần này");
            }
            throw error;
        }
    }

    @Override
    public void huyDangKy(String token, String maLhp) throws BusinessException {
        sessions.require(token, VaiTro.SINH_VIEN);
        String code = text(maLhp, 30, "Mã lớp học phần");
        retryTransaction(config.soLanThuLai(), em -> {
            var session = sessions.require(token, VaiTro.SINH_VIEN);
            SinhVien sv = em.find(SinhVien.class, session.tenDangNhap(), LockModeType.PESSIMISTIC_WRITE);
            if (sv == null) sv = find(em, SinhVien.class, session.tenDangNhap());
            LopHocPhan lhp = find(em, LopHocPhan.class, code);
            if (!lhp.getHocKy().equals(config.hocKyDangKy())) {
                throw new BusinessException(ErrorCode.NGOAI_HOC_KY_DANG_KY, "Không được hủy đăng ký ngoài học kỳ đang mở");
            }
            DangKyHocPhan registration = find(em, DangKyHocPhan.class, new DangKyId(sv.getMaSv(), lhp.getMaLhp()));
            if (registration.getDiemSo() != null) {
                throw new BusinessException(ErrorCode.RANG_BUOC, "Không được hủy học phần đã có điểm");
            }
            em.remove(registration);
            lhp.setSoLuongDaDk(lhp.getSoLuongDaDk() - 1);
            return null;
        });
    }

    @Override
    public List<DangKyDTO> layDangKyCuaToi(String token) throws BusinessException {
        var session = sessions.require(token, VaiTro.SINH_VIEN);
        return transaction(em -> HocTapQueries.history(em, session.tenDangNhap()).stream()
                .map(HocTapMapper::dangKy).toList());
    }

    @Override
    public List<LopCoTheDangKyDTO> layLopCoTheDangKy(String token) throws BusinessException {
        var session = sessions.require(token, VaiTro.SINH_VIEN);
        return transaction(em -> {
            SinhVien sv = find(em, SinhVien.class, session.tenDangNhap());
            List<DangKyDTO> history = HocTapQueries.history(em, sv.getMaSv()).stream()
                    .map(HocTapMapper::dangKy).toList();
            em.createQuery("select distinct m from MonHoc m left join fetch m.monTienQuyet", MonHoc.class)
                    .getResultList();
            var result = new java.util.ArrayList<LopCoTheDangKyDTO>();
            for (LopHocPhan lhp : HocTapQueries.sections(em, config.hocKyDangKy())) {
                try {
                    validate(sv, lhp, history);
                    result.add(new LopCoTheDangKyDTO(HocTapMapper.lop(lhp), true, null, null));
                } catch (BusinessException error) {
                    result.add(new LopCoTheDangKyDTO(HocTapMapper.lop(lhp), false, error.getCode(), error.getMessage()));
                }
            }
            return List.copyOf(result);
        });
    }

    private void validate(SinhVien sv, LopHocPhan lhp, List<DangKyDTO> history) throws BusinessException {
        DangKyRules.validate(sv.getTrangThai(), config.hocKyDangKy(), config.tinChiToiDa(), config.diemQua(),
                HocTapMapper.lop(lhp), lhp.getMonHoc().getMonTienQuyet().stream()
                        .map(MonHoc::getMaMon).collect(Collectors.toSet()), history);
    }
}
