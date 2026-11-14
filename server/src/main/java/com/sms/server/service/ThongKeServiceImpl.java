package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.*;
import com.sms.common.enums.TrangThaiHocTap;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.ThongKeService;
import com.sms.server.entity.LopHocPhan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import java.util.*;

public final class ThongKeServiceImpl extends ServiceSupport implements ThongKeService {

    public ThongKeServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public List<ThongKeKhoaDTO> soSinhVienTheoKhoa(String token) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        return transaction(em -> em.createQuery("""
                select new com.sms.common.dto.ThongKeKhoaDTO(k.maKhoa, k.tenKhoa, count(s))
                from Khoa k left join LopHoc l on l.khoa = k left join SinhVien s on s.lopHoc = l
                group by k.maKhoa, k.tenKhoa order by k.maKhoa
                """, ThongKeKhoaDTO.class).getResultList());
    }

    @Override
    public List<ThongKeTrangThaiDTO> soSinhVienTheoTrangThai(String token) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        return transaction(em -> {
            Map<TrangThaiHocTap, Long> totals = new EnumMap<>(TrangThaiHocTap.class);
            for (Object[] row : em.createQuery(
                    "select s.trangThai, count(s) from SinhVien s group by s.trangThai", Object[].class)
                    .getResultList()) {
                totals.put((TrangThaiHocTap) row[0], (Long) row[1]);
            }
            return Arrays.stream(TrangThaiHocTap.values())
                    .map(status -> new ThongKeTrangThaiDTO(status, totals.getOrDefault(status, 0L))).toList();
        });
    }

    @Override
    public List<PhanBoDiemDTO> phanBoDiemTheoLopHocPhan(String token, String maLhp) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maLhp, 30, "Mã lớp học phần");
        return transaction(em -> {
            find(em, LopHocPhan.class, code);
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String label : List.of("A+", "A", "B+", "B", "C+", "C", "D+", "D", "F", "Chưa có điểm")) {
                counts.put(label, 0L);
            }
            for (Double grade : em.createQuery(
                    "select d.diemSo from DangKyHocPhan d where d.lopHocPhan.maLhp = :code", Double.class)
                    .setParameter("code", code).getResultList()) {
                String label = grade == null ? "Chưa có điểm" : GpaCalculator.quyDoi(grade).diemChu();
                counts.merge(label, 1L, Long::sum);
            }
            return counts.entrySet().stream().map(e -> new PhanBoDiemDTO(e.getKey(), e.getValue())).toList();
        });
    }

    @Override
    public List<SiSoLopHocPhanDTO> siSoLopHocPhan(String token, String hocKy) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> em.createQuery("""
                select new com.sms.common.dto.SiSoLopHocPhanDTO(
                    l.maLhp, l.monHoc.tenMon, l.hocKy, l.soLuongDaDk, l.soLuongToiDa)
                from LopHocPhan l where l.hocKy = :semester order by l.maLhp
                """, SiSoLopHocPhanDTO.class).setParameter("semester", semester).getResultList());
    }
}
