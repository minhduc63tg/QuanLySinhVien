package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.*;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.TrangThaiHocTap;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.DiemService;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.HocTapMapper;
import com.sms.server.security.SessionInfo;
import com.sms.server.security.SessionManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.util.stream.Collectors;

public final class DiemServiceImpl extends ServiceSupport implements DiemService {

    private final AppConfig config;

    public DiemServiceImpl(JpaUtil jpa, SessionManager sessions, AppConfig config) {
        super(jpa, sessions);
        this.config = config;
    }

    @Override
    public BangDiemDTO layBangDiemCuaToi(String token) throws BusinessException {
        var session = sessions.require(token, VaiTro.SINH_VIEN);
        return transaction(em -> bangDiem(em, session.tenDangNhap()));
    }

    @Override
    public BangDiemDTO layBangDiem(String token, String maSv) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String student = text(maSv, 20, "Mã sinh viên");
        return transaction(em -> {
            find(em, SinhVien.class, student); // báo KHONG_TIM_THAY nếu không có sinh viên
            return bangDiem(em, student);
        });
    }

    /** Bảng điểm theo học kỳ (mới nhất trước) và GPA tích lũy của một sinh viên. */
    private static BangDiemDTO bangDiem(jakarta.persistence.EntityManager em, String maSv) {
        List<DangKyHocPhan> history = HocTapQueries.history(em, maSv);
        var groups = history.stream().collect(Collectors.groupingBy(
                d -> d.getLopHocPhan().getHocKy(), () -> new TreeMap<String, List<DangKyHocPhan>>(
                        Comparator.reverseOrder()), Collectors.toList()));
        List<BangDiemHocKyDTO> semesters = groups.entrySet().stream().map(entry ->
                new BangDiemHocKyDTO(entry.getKey(), entry.getValue().stream()
                        .map(HocTapMapper::diem).toList(), gpa(entry.getValue()))).toList();
        return new BangDiemDTO(semesters, gpa(history));
    }

    @Override
    public List<LopHocPhanDTO> layLopDayCuaToi(String token, String hocKy) throws BusinessException {
        var session = sessions.require(token, VaiTro.GIANG_VIEN);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> HocTapQueries.sections(em, semester).stream()
                .filter(l -> l.getGiangVien() != null
                        && l.getGiangVien().getMaGv().equals(session.tenDangNhap()))
                .map(HocTapMapper::lop).toList());
    }

    @Override
    public List<DiemDTO> layDanhSachSvTrongLop(String token, String maLhp) throws BusinessException {
        var session = sessions.require(token, VaiTro.GIANG_VIEN, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        return transaction(em -> {
            authorize(session, find(em, LopHocPhan.class, code));
            return em.createQuery("""
                    select d from DangKyHocPhan d join fetch d.sinhVien
                    join fetch d.lopHocPhan l join fetch l.monHoc
                    where l.maLhp = :code order by d.sinhVien.maSv
                    """, DangKyHocPhan.class).setParameter("code", code).getResultList().stream()
                    .map(HocTapMapper::diem).toList();
        });
    }

    @Override
    public void nhapDiem(String token, String maLhp, String maSv, double diem) throws BusinessException {
        sessions.require(token, VaiTro.GIANG_VIEN, VaiTro.QUAN_LY);
        String student = text(maSv, 20, "Mã sinh viên");
        nhapDiemHangLoat(token, maLhp, Map.of(student, diem));
    }

    @Override
    public void nhapDiemHangLoat(String token, String maLhp, Map<String, Double> diemTheoMaSv)
            throws BusinessException {
        var session = sessions.require(token, VaiTro.GIANG_VIEN, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        check(diemTheoMaSv != null && !diemTheoMaSv.isEmpty(), "Danh sách điểm không được trống");
        Map<String, Double> scores = new TreeMap<>();
        for (var entry : diemTheoMaSv.entrySet()) {
            String student = text(entry.getKey(), 20, "Mã sinh viên");
            Double grade = entry.getValue();
            check(grade != null && Double.isFinite(grade) && grade >= 0 && grade <= 10,
                    "Điểm phải nằm trong [0, 10]");
            check(scores.put(student, GpaCalculator.lamTron(grade)) == null, "Mã sinh viên bị lặp");
        }
        transaction(em -> {
            Map<String, SinhVien> students = new TreeMap<>();
            // Khóa sinh viên trước, cùng thứ tự với đăng ký/hủy; tránh ghi GPA từ snapshot cũ.
            for (String student : scores.keySet()) {
                SinhVien sv = em.find(SinhVien.class, student, LockModeType.PESSIMISTIC_WRITE);
                if (sv == null) sv = find(em, SinhVien.class, student);
                check(students.put(sv.getMaSv(), sv) == null, "Mã sinh viên bị lặp");
            }
            LopHocPhan lhp = em.find(LopHocPhan.class, code, LockModeType.PESSIMISTIC_READ);
            if (lhp == null) lhp = find(em, LopHocPhan.class, code);
            authorize(session, lhp);
            for (var entry : scores.entrySet()) {
                SinhVien sv = find(em, SinhVien.class, entry.getKey());
                DangKyHocPhan registration = find(em, DangKyHocPhan.class,
                        new DangKyId(sv.getMaSv(), lhp.getMaLhp()));
                registration.setDiemSo(entry.getValue());
            }
            em.flush();
            List<DangKyHocPhan> history = em.createQuery("""
                    select d from DangKyHocPhan d join fetch d.sinhVien
                    join fetch d.lopHocPhan l join fetch l.monHoc
                    where d.sinhVien.maSv in :students
                    """, DangKyHocPhan.class).setParameter("students", students.keySet()).getResultList();
            Map<String, List<DangKyHocPhan>> grouped = history.stream()
                    .collect(Collectors.groupingBy(d -> d.getSinhVien().getMaSv()));
            for (SinhVien sv : students.values()) {
                if (sv.getTrangThai() != TrangThaiHocTap.TOT_NGHIEP) {
                    Double gpa = gpa(grouped.getOrDefault(sv.getMaSv(), List.of()));
                    sv.setTrangThai(gpa != null && gpa < config.canhBaoGpa()
                            ? TrangThaiHocTap.CANH_BAO : TrangThaiHocTap.DANG_HOC);
                }
            }
            return null;
        });
    }

    private static Double gpa(List<DangKyHocPhan> registrations) {
        return GpaCalculator.tinhGpa(registrations.stream().map(HocTapMapper::diemMon).toList());
    }

    private static void authorize(SessionInfo session, LopHocPhan lhp) throws BusinessException {
        if (session.vaiTro() != VaiTro.QUAN_LY && (lhp.getGiangVien() == null
                || !lhp.getGiangVien().getMaGv().equals(session.tenDangNhap()))) {
            throw new BusinessException(ErrorCode.KHONG_CO_QUYEN, "Bạn không được phân công lớp học phần này");
        }
    }
}
