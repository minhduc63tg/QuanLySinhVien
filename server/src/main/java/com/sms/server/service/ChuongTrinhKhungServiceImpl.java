package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.ChuongTrinhKhungDTO;
import com.sms.common.dto.MonChuongTrinhDTO;
import com.sms.common.dto.TienDoHocTapDTO;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.ChuongTrinhKhungService;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;

public final class ChuongTrinhKhungServiceImpl extends ServiceSupport implements ChuongTrinhKhungService {

    private static final int MAX_TONG_TIN_CHI = 500;

    private final AppConfig config;

    public ChuongTrinhKhungServiceImpl(JpaUtil jpa, SessionManager sessions, AppConfig config) {
        super(jpa, sessions);
        this.config = config;
    }

    @Override
    public ChuongTrinhKhungDTO layChuongTrinhKhung(String token, String maKhoa) throws BusinessException {
        sessions.require(token);
        String code = text(maKhoa, 20, "Mã khoa");
        return transaction(em -> load(em, find(em, Khoa.class, code)));
    }

    @Override
    public ChuongTrinhKhungDTO luuChuongTrinhKhung(String token, ChuongTrinhKhungDTO dto) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Chương trình khung không được null");
        String code = text(dto.maKhoa(), 20, "Mã khoa");
        check(dto.tongTinChi() >= 0 && dto.tongTinChi() <= MAX_TONG_TIN_CHI,
                "Tổng số tín chỉ phải từ 0 đến " + MAX_TONG_TIN_CHI);
        List<MonChuongTrinhDTO> items = dto.monHoc() == null ? List.of() : dto.monHoc();
        Set<String> seen = new HashSet<>();
        for (MonChuongTrinhDTO item : items) {
            check(item != null && item.maMon() != null && !item.maMon().isBlank(), "Mã môn không được trống");
            check(seen.add(item.maMon().trim()), "Môn " + item.maMon() + " bị khai báo trùng");
            check(item.hocKyGoiY() >= 1 && item.hocKyGoiY() <= 12, "Học kỳ gợi ý phải từ 1 đến 12");
        }
        return transaction(em -> {
            // Khóa khoa để hai lần lưu đồng thời không đan xen các dòng môn
            Khoa khoa = em.find(Khoa.class, code, LockModeType.PESSIMISTIC_WRITE);
            if (khoa == null) khoa = find(em, Khoa.class, code);
            ChuongTrinhKhung program = em.find(ChuongTrinhKhung.class, code);
            if (program == null) {
                program = new ChuongTrinhKhung();
                program.setKhoa(khoa);
                em.persist(program);
            }
            int mandatory = 0;
            List<ChuongTrinhKhungMon> rows = new ArrayList<>();
            for (MonChuongTrinhDTO item : items) {
                MonHoc mon = find(em, MonHoc.class, item.maMon().trim());
                ChuongTrinhKhungMon row = new ChuongTrinhKhungMon();
                row.setMonHoc(mon);
                row.setHocKyGoiY(item.hocKyGoiY());
                row.setBatBuoc(item.batBuoc());
                rows.add(row);
                if (item.batBuoc()) mandatory += mon.getSoTinChi();
            }
            check(dto.tongTinChi() >= mandatory,
                    "Tổng số tín chỉ (" + dto.tongTinChi() + ") không được nhỏ hơn tổng tín chỉ môn bắt buộc ("
                            + mandatory + ")");
            program.setTongTinChi(dto.tongTinChi());
            // Xóa và flush trước khi thêm lại: Hibernate chèn trước khi xóa sẽ vi phạm unique (ma_khoa, ma_mon)
            program.getMonHoc().clear();
            em.flush();
            rows.forEach(program::themMon);
            em.flush();
            return load(em, khoa);
        });
    }

    @Override
    public TienDoHocTapDTO layTienDoCuaToi(String token) throws BusinessException {
        var session = sessions.require(token, VaiTro.SINH_VIEN);
        return transaction(em -> {
            SinhVien sv = find(em, SinhVien.class, session.tenDangNhap());
            ChuongTrinhKhungDTO program = load(em, sv.getLopHoc().getKhoa());
            Map<String, List<Double>> attempts = new HashMap<>();
            for (DangKyHocPhan registration : HocTapQueries.history(em, sv.getMaSv())) {
                attempts.computeIfAbsent(registration.getLopHocPhan().getMonHoc().getMaMon(),
                        k -> new ArrayList<>()).add(registration.getDiemSo());
            }
            return TienDoCalculator.tinh(program, attempts, config.diemQua());
        });
    }

    private static ChuongTrinhKhungDTO load(EntityManager em, Khoa khoa) {
        List<MonChuongTrinhDTO> items = em.createQuery("""
                select m from ChuongTrinhKhungMon m join fetch m.monHoc
                where m.chuongTrinh.maKhoa = :khoa
                order by m.hocKyGoiY, m.batBuoc desc, m.monHoc.maMon
                """, ChuongTrinhKhungMon.class).setParameter("khoa", khoa.getMaKhoa()).getResultStream()
                .map(m -> new MonChuongTrinhDTO(m.getMonHoc().getMaMon(), m.getMonHoc().getTenMon(),
                        m.getMonHoc().getSoTinChi(), m.getHocKyGoiY(), m.isBatBuoc(), null, null))
                .toList();
        ChuongTrinhKhung program = em.find(ChuongTrinhKhung.class, khoa.getMaKhoa());
        return new ChuongTrinhKhungDTO(khoa.getMaKhoa(), khoa.getTenKhoa(),
                program == null ? 0 : program.getTongTinChi(), items);
    }
}
