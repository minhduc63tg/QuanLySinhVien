package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichHocDTO;
import com.sms.common.dto.LopHocPhanDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.LopHocPhanService;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.HocTapMapper;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;

public final class LopHocPhanServiceImpl extends ServiceSupport implements LopHocPhanService {

    public LopHocPhanServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public LopHocPhanDTO themLopHocPhan(String token, LopHocPhanDTO dto) throws BusinessException {
        return save(token, dto, true);
    }

    @Override
    public LopHocPhanDTO suaLopHocPhan(String token, LopHocPhanDTO dto) throws BusinessException {
        return save(token, dto, false);
    }

    private LopHocPhanDTO save(String token, LopHocPhanDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Lớp học phần không được null");
        // Thêm mới mà để trống mã thì server tự sinh: mã môn + số thứ tự trong học kỳ
        String requested = create && (dto.maLhp() == null || dto.maLhp().isBlank()) ? null
                : text(dto.maLhp(), 30, "Mã lớp học phần");
        String course = text(dto.maMon(), 20, "Mã môn");
        String teacher = HoSoSupport.optional(dto.maGv(), 20, "Mã giảng viên");
        String semester = text(dto.hocKy(), 20, "Học kỳ");
        check(dto.soLuongToiDa() > 0, "Sĩ số tối đa phải dương");
        if (create) LichHocRules.validate(dto.lichHoc());
        return transaction(em -> {
            lockSchedule(em);
            MonHoc mon = find(em, MonHoc.class, course);
            String code = requested != null ? requested : MaLopHocPhan.tiepTheo(mon.getMaMon(),
                    em.createQuery("select count(l) from LopHocPhan l where l.monHoc.maMon = :mon and l.hocKy = :hocKy",
                            Long.class).setParameter("mon", mon.getMaMon()).setParameter("hocKy", semester)
                            .getSingleResult(),
                    candidate -> em.find(LopHocPhan.class, candidate) != null);
            GiangVien gv = teacher == null ? null : find(em, GiangVien.class, teacher);
            LopHocPhan lhp = em.find(LopHocPhan.class, code, LockModeType.PESSIMISTIC_WRITE);
            if (create && lhp != null) {
                throw new BusinessException(ErrorCode.DA_TON_TAI, "Mã lớp học phần đã tồn tại");
            }
            if (!create && lhp == null) lhp = find(em, LopHocPhan.class, code);
            if (create) {
                lhp = new LopHocPhan();
                lhp.setMaLhp(code);
            }
            check(dto.soLuongToiDa() >= lhp.getSoLuongDaDk(),
                    "Sĩ số tối đa không được nhỏ hơn số đã đăng ký");
            if (lhp.getSoLuongDaDk() > 0
                    && (!lhp.getMonHoc().getMaMon().equals(mon.getMaMon()) || !lhp.getHocKy().equals(semester))) {
                throw new BusinessException(ErrorCode.RANG_BUOC,
                        "Không được đổi môn hoặc học kỳ của lớp đã có đăng ký");
            }
            List<LichHocDTO> schedule = create ? dto.lichHoc()
                    : lhp.getLichHoc().stream().map(HocTapMapper::lich).toList();
            validateConflicts(em, lhp.getMaLhp(), semester, gv, schedule);
            lhp.setMonHoc(mon);
            lhp.setGiangVien(gv);
            lhp.setHocKy(semester);
            lhp.setSoLuongToiDa(dto.soLuongToiDa());
            if (create) {
                for (LichHocDTO slot : schedule) lhp.themLichHoc(toEntity(slot));
                em.persist(lhp);
            }
            em.flush();
            return HocTapMapper.lop(lhp);
        });
    }

    @Override
    public void luuLichHoc(String token, String maLhp, List<LichHocDTO> lichHoc) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        LichHocRules.validate(lichHoc);
        List<LichHocDTO> schedule = List.copyOf(lichHoc);
        transaction(em -> {
            lockSchedule(em);
            LopHocPhan lhp = em.find(LopHocPhan.class, code, LockModeType.PESSIMISTIC_WRITE);
            if (lhp == null) lhp = find(em, LopHocPhan.class, code);
            validateConflicts(em, lhp.getMaLhp(), lhp.getHocKy(), lhp.getGiangVien(), schedule);
            em.lock(lhp, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            for (LichHoc old : new ArrayList<>(lhp.getLichHoc())) lhp.xoaLichHoc(old);
            for (LichHocDTO slot : schedule) lhp.themLichHoc(toEntity(slot));
            return null;
        });
    }

    @Override
    public void xoaLopHocPhan(String token, String maLhp) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        transaction(em -> {
            lockSchedule(em);
            LopHocPhan lhp = em.find(LopHocPhan.class, code, LockModeType.PESSIMISTIC_WRITE);
            if (lhp == null) lhp = find(em, LopHocPhan.class, code);
            noDependency(em.createQuery(
                    "select count(d) from DangKyHocPhan d where d.lopHocPhan.maLhp = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            // Lịch thi thuộc về lớp học phần nên xóa cùng lớp
            em.createQuery("select t from LichThi t where t.lopHocPhan.maLhp = :code", LichThi.class)
                    .setParameter("code", code).getResultList().forEach(em::remove);
            em.flush();
            em.remove(lhp);
            return null;
        });
    }

    @Override
    public List<LopHocPhanDTO> timTheoHocKy(String token, String hocKy) throws BusinessException {
        sessions.require(token);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> HocTapQueries.sections(em, semester).stream().map(HocTapMapper::lop).toList());
    }

    static void lockSchedule(EntityManager em) {
        em.createQuery("select m from MonHoc m order by m.maMon", MonHoc.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    private static void validateConflicts(EntityManager em, String code, String semester,
            GiangVien teacher, List<LichHocDTO> schedule) throws BusinessException {
        for (LopHocPhan other : HocTapQueries.sections(em, semester)) {
            if (other.getMaLhp().equals(code)) continue;
            for (LichHocDTO slot : schedule) {
                for (LichHoc old : other.getLichHoc()) {
                    if (!LichHocRules.overlaps(slot, HocTapMapper.lich(old))) continue;
                    if (slot.phong().trim().equalsIgnoreCase(old.getPhong())
                            || teacher != null && other.getGiangVien() != null
                            && teacher.getMaGv().equals(other.getGiangVien().getMaGv())) {
                        throw new BusinessException(ErrorCode.TRUNG_LICH,
                                "Trùng lịch phòng học hoặc giảng viên trong học kỳ");
                    }
                }
            }
        }
    }

    private static LichHoc toEntity(LichHocDTO dto) {
        LichHoc slot = new LichHoc();
        slot.setThu(dto.thu());
        slot.setTietBatDau(dto.tietBatDau());
        slot.setSoTiet(dto.soTiet());
        slot.setPhong(dto.phong().trim());
        return slot;
    }
}
