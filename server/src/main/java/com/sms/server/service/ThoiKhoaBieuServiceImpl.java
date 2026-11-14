package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.ThoiKhoaBieuDTO;
import com.sms.common.dto.TietHocDTO;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.ThoiKhoaBieuService;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;

public final class ThoiKhoaBieuServiceImpl extends ServiceSupport implements ThoiKhoaBieuService {

    public ThoiKhoaBieuServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public ThoiKhoaBieuDTO thoiKhoaBieuSinhVien(String token, String hocKy) throws BusinessException {
        var info = sessions.require(token, VaiTro.SINH_VIEN);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> new ThoiKhoaBieuDTO(semester, em.createQuery("""
                select new com.sms.common.dto.TietHocDTO(l.maLhp, m.maMon, m.tenMon,
                    g.maGv, g.hoTen, h.thu, h.tietBatDau, h.soTiet, h.phong)
                from DangKyHocPhan d join d.lopHocPhan l join l.monHoc m
                left join l.giangVien g join l.lichHoc h
                where d.sinhVien.maSv = :user and l.hocKy = :semester
                order by h.thu, h.tietBatDau, l.maLhp
                """, TietHocDTO.class).setParameter("user", info.tenDangNhap())
                .setParameter("semester", semester).getResultList()));
    }

    @Override
    public ThoiKhoaBieuDTO thoiKhoaBieuGiangVien(String token, String hocKy) throws BusinessException {
        var info = sessions.require(token, VaiTro.GIANG_VIEN);
        String semester = text(hocKy, 20, "Học kỳ");
        return transaction(em -> new ThoiKhoaBieuDTO(semester, em.createQuery("""
                select new com.sms.common.dto.TietHocDTO(l.maLhp, m.maMon, m.tenMon,
                    g.maGv, g.hoTen, h.thu, h.tietBatDau, h.soTiet, h.phong)
                from LopHocPhan l join l.monHoc m join l.giangVien g join l.lichHoc h
                where g.maGv = :user and l.hocKy = :semester
                order by h.thu, h.tietBatDau, l.maLhp
                """, TietHocDTO.class).setParameter("user", info.tenDangNhap())
                .setParameter("semester", semester).getResultList()));
    }
}
