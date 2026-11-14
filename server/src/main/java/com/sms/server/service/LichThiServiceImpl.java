package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichThiDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.LichThiService;
import com.sms.server.entity.GiangVien;
import com.sms.server.entity.LichThi;
import com.sms.server.entity.LopHocPhan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LichThiServiceImpl extends ServiceSupport implements LichThiService {

    private static final String SELECT = """
            select distinct t from LichThi t join fetch t.lopHocPhan l join fetch l.monHoc
            left join fetch t.giamThi
            """;

    private static final Comparator<LichThiDTO> ORDER = Comparator.comparing(LichThiDTO::ngayThi)
            .thenComparingInt(LichThiDTO::tietBatDau).thenComparing(LichThiDTO::maLhp);

    public LichThiServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public LichThiDTO luuLichThi(String token, LichThiDTO dto) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        List<String> invigilators = LichThiRules.validate(dto);
        String code = text(dto.maLhp(), 30, "Mã lớp học phần");
        String room = dto.phong().trim();
        return transaction(em -> {
            // Cùng khóa với việc xếp lịch học để hai lần xếp đồng thời không lọt qua kiểm tra trùng
            LopHocPhanServiceImpl.lockSchedule(em);
            LopHocPhan lhp = find(em, LopHocPhan.class, code);
            Set<GiangVien> teachers = new HashSet<>();
            for (String maGv : invigilators) {
                teachers.add(find(em, GiangVien.class, maGv));
            }
            checkConflicts(em, lhp, dto, room, invigilators);
            LichThi exam = em.createQuery("select t from LichThi t where t.lopHocPhan.maLhp = :code", LichThi.class)
                    .setParameter("code", code).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultStream().findFirst().orElse(null);
            if (exam == null) {
                exam = new LichThi();
                exam.setLopHocPhan(lhp);
            }
            exam.setNgayThi(dto.ngayThi());
            exam.setTietBatDau(dto.tietBatDau());
            exam.setSoTiet(dto.soTiet());
            exam.setPhong(room);
            exam.getGiamThi().clear();
            exam.getGiamThi().addAll(teachers);
            if (exam.getId() == null) em.persist(exam);
            em.flush();
            return toDto(exam);
        });
    }

    /** Một buổi thi không được trùng phòng, giám thị hoặc sinh viên với buổi thi khác cùng ngày và giờ. */
    private static void checkConflicts(EntityManager em, LopHocPhan lhp, LichThiDTO dto, String room,
            List<String> invigilators) throws BusinessException {
        List<LichThi> sameDay = em.createQuery(SELECT + " where t.ngayThi = :day and l.maLhp <> :code",
                LichThi.class).setParameter("day", dto.ngayThi()).setParameter("code", lhp.getMaLhp())
                .getResultList();
        for (LichThi other : sameDay) {
            if (!LichThiRules.overlaps(dto.ngayThi(), dto.tietBatDau(), dto.soTiet(),
                    other.getNgayThi(), other.getTietBatDau(), other.getSoTiet())) {
                continue;
            }
            String label = " (lớp " + other.getLopHocPhan().getMaLhp() + ")";
            if (room.equalsIgnoreCase(other.getPhong())) {
                throw new BusinessException(ErrorCode.TRUNG_LICH, "Phòng thi " + room + " đã có buổi thi khác" + label);
            }
            Set<String> otherCodes = new HashSet<>();
            other.getGiamThi().forEach(g -> otherCodes.add(g.getMaGv()));
            Set<String> shared = LichThiRules.intersection(new HashSet<>(invigilators), otherCodes);
            if (!shared.isEmpty()) {
                throw new BusinessException(ErrorCode.TRUNG_LICH,
                        "Giám thị " + shared.iterator().next() + " đã gác buổi thi khác" + label);
            }
            long students = em.createQuery("""
                    select count(d) from DangKyHocPhan d where d.lopHocPhan.maLhp = :other
                    and d.sinhVien.maSv in (select d2.sinhVien.maSv from DangKyHocPhan d2
                                            where d2.lopHocPhan.maLhp = :code)
                    """, Long.class).setParameter("other", other.getLopHocPhan().getMaLhp())
                    .setParameter("code", lhp.getMaLhp()).getSingleResult();
            if (students > 0) {
                throw new BusinessException(ErrorCode.TRUNG_LICH,
                        "Có " + students + " sinh viên phải thi trùng giờ với buổi thi khác" + label);
            }
        }
    }

    @Override
    public void xoaLichThi(String token, String maLhp) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        transaction(em -> {
            LichThi exam = em.createQuery("select t from LichThi t where t.lopHocPhan.maLhp = :code", LichThi.class)
                    .setParameter("code", code).getResultStream().findFirst().orElse(null);
            if (exam == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Lớp học phần chưa có lịch thi");
            }
            em.remove(exam);
            return null;
        });
    }

    @Override
    public List<LichThiDTO> layLichThiTheoHocKy(String token, String hocKy) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> em.createQuery(SELECT + " where l.hocKy = :semester", LichThi.class)
                .setParameter("semester", semester).getResultStream().map(LichThiServiceImpl::toDto)
                .sorted(ORDER).toList());
    }

    @Override
    public List<LichThiDTO> lichThiSinhVien(String token, String hocKy) throws BusinessException {
        var info = sessions.require(token, VaiTro.SINH_VIEN);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> em.createQuery(SELECT + """
                where l.hocKy = :semester and l.maLhp in
                    (select d.lopHocPhan.maLhp from DangKyHocPhan d where d.sinhVien.maSv = :user)
                """, LichThi.class).setParameter("semester", semester).setParameter("user", info.tenDangNhap())
                .getResultStream().map(LichThiServiceImpl::toDto).sorted(ORDER).toList());
    }

    @Override
    public List<LichThiDTO> lichGacThi(String token, String hocKy) throws BusinessException {
        var info = sessions.require(token, VaiTro.GIANG_VIEN);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> em.createQuery(SELECT + """
                where l.hocKy = :semester and t.id in
                    (select t2.id from LichThi t2 join t2.giamThi g2 where g2.maGv = :user)
                """, LichThi.class).setParameter("semester", semester).setParameter("user", info.tenDangNhap())
                .getResultStream().map(LichThiServiceImpl::toDto).sorted(ORDER).toList());
    }

    static LichThiDTO toDto(LichThi exam) {
        List<GiangVien> teachers = exam.getGiamThi().stream()
                .sorted(Comparator.comparing(GiangVien::getMaGv)).toList();
        LopHocPhan lhp = exam.getLopHocPhan();
        return new LichThiDTO(lhp.getMaLhp(), lhp.getMonHoc().getMaMon(), lhp.getMonHoc().getTenMon(),
                lhp.getHocKy(), exam.getNgayThi(), exam.getTietBatDau(), exam.getSoTiet(), exam.getPhong(),
                teachers.stream().map(GiangVien::getMaGv).toList(),
                teachers.stream().map(GiangVien::getHoTen).toList(), lhp.getSoLuongDaDk());
    }
}
