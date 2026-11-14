package com.sms.server.service;

import com.sms.server.entity.DangKyHocPhan;
import com.sms.server.entity.LopHocPhan;
import jakarta.persistence.EntityManager;
import java.util.List;

final class HocTapQueries {

    private HocTapQueries() {
    }

    static List<LopHocPhan> sections(EntityManager em, String semester) {
        return em.createQuery("""
                select distinct l from LopHocPhan l join fetch l.monHoc
                left join fetch l.giangVien left join fetch l.lichHoc
                where (:semester is null or l.hocKy = :semester) order by l.maLhp
                """, LopHocPhan.class).setParameter("semester", semester).getResultList();
    }

    static List<DangKyHocPhan> history(EntityManager em, String student) {
        return em.createQuery("""
                select distinct d from DangKyHocPhan d join fetch d.sinhVien
                join fetch d.lopHocPhan l join fetch l.monHoc
                left join fetch l.giangVien left join fetch l.lichHoc
                where d.sinhVien.maSv = :student order by d.ngayDangKy, d.id.maLhp
                """, DangKyHocPhan.class).setParameter("student", student).getResultList();
    }
}
