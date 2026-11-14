package com.sms.server.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.*;
import com.sms.common.enums.*;
import com.sms.common.rmi.*;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.PasswordUtil;
import com.sms.server.security.SessionManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Chương trình khung và tiến độ học tập qua RMI thật với MariaDB. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class ChuongTrinhKhungMariaDbTest {

    @TempDir
    Path temporary;

    @FunctionalInterface
    private interface Call { void run() throws Exception; }

    private static void expect(ErrorCode code, Call call) {
        assertEquals(code, assertThrows(BusinessException.class, call::run).getCode());
    }

    @Test
    void savesProgramAndComputesStudentProgress() throws Exception {
        String p = "CT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        AppConfig config = testConfig(p);
        String previousHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        try (JpaUtil jpa = new JpaUtil(config); SessionManager sessions = new SessionManager();
                RmiServer rmi = new RmiServer(0, 0, jpa, sessions, config)) {
            try {
                run(p, jpa, rmi);
            } finally {
                cleanup(p, jpa);
            }
        } finally {
            if (previousHost == null) System.clearProperty("java.rmi.server.hostname");
            else System.setProperty("java.rmi.server.hostname", previousHost);
        }
    }

    private void run(String p, JpaUtil jpa, RmiServer rmi) throws Exception {
        var registry = rmi.registryStub();
        AuthService auth = (AuthService) registry.lookup(RmiNames.AUTH_SERVICE);
        DanhMucService catalog = (DanhMucService) registry.lookup(RmiNames.DANH_MUC_SERVICE);
        MonHocService courses = (MonHocService) registry.lookup(RmiNames.MON_HOC_SERVICE);
        SinhVienService students = (SinhVienService) registry.lookup(RmiNames.SINH_VIEN_SERVICE);
        ChuongTrinhKhungService programs = (ChuongTrinhKhungService) registry.lookup(RmiNames.CHUONG_TRINH_KHUNG_SERVICE);

        jpa.inTransaction(em -> {
            TaiKhoan account = new TaiKhoan();
            account.setTenDangNhap(p + "AD");
            account.setMatKhau(PasswordUtil.hash("Test@123"));
            account.setVaiTro(VaiTro.QUAN_LY);
            em.persist(account);
            return null;
        });
        String admin = auth.dangNhap(p + "AD", "Test@123").token();
        catalog.themKhoa(admin, new KhoaDTO(p + "K", "Khoa kiểm thử"));
        catalog.themKhoa(admin, new KhoaDTO(p + "K2", "Khoa trống"));
        catalog.themLop(admin, new LopHocDTO(p + "L", "Lớp kiểm thử", p + "K", null));
        students.themSinhVien(admin, new SinhVienDTO(p + "S", "Sinh viên kiểm thử", LocalDate.of(2004, 1, 1),
                GioiTinh.NAM, null, null, TrangThaiHocTap.DANG_HOC, p + "L", null, null, null));
        for (String suffix : List.of("A", "B", "C", "D")) {
            courses.themMon(admin, new MonHocDTO(p + suffix, "Môn " + suffix, suffix.equals("B") ? 4 : 3, Set.of()));
        }
        String sv = auth.dangNhap(p + "S", p + "S").token();

        // Khoa chưa có chương trình: tổng 0, danh sách rỗng; tiến độ không lỗi
        ChuongTrinhKhungDTO none = programs.layChuongTrinhKhung(sv, p + "K");
        assertEquals(0, none.tongTinChi());
        assertTrue(none.monHoc().isEmpty());
        assertEquals(0, programs.layTienDoCuaToi(sv).tinChiYeuCau());

        // Kiểm tra dữ liệu không hợp lệ
        MonChuongTrinhDTO a = new MonChuongTrinhDTO(p + "A", null, 0, 1, true, null, null);
        MonChuongTrinhDTO b = new MonChuongTrinhDTO(p + "B", null, 0, 1, true, null, null);
        MonChuongTrinhDTO c = new MonChuongTrinhDTO(p + "C", null, 0, 2, true, null, null);
        MonChuongTrinhDTO d = new MonChuongTrinhDTO(p + "D", null, 0, 3, false, null, null);
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 5, List.of(a, b))));      // 5 < 3 + 4 bắt buộc
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 50, List.of(a, a))));     // trùng môn
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 50, List.of(
                        new MonChuongTrinhDTO(p + "A", null, 0, 0, true, null, null))))); // học kỳ 0
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 501, List.of())));
        expect(ErrorCode.KHONG_TIM_THAY, () -> programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 50, List.of(
                        new MonChuongTrinhDTO(p + "KHONG-CO", null, 0, 1, true, null, null)))));
        expect(ErrorCode.KHONG_TIM_THAY, () -> programs.layChuongTrinhKhung(admin, p + "KHONG-CO"));
        assertEquals(0, programs.layChuongTrinhKhung(admin, p + "K").tongTinChi(), "lưu lỗi không để lại dữ liệu");

        // Lưu hợp lệ, lưu lại (thay thế) không vi phạm unique
        ChuongTrinhKhungDTO saved = programs.luuChuongTrinhKhung(admin,
                new ChuongTrinhKhungDTO(p + "K", null, 130, List.of(d, c, b, a)));
        assertEquals(130, saved.tongTinChi());
        assertEquals(List.of(p + "A", p + "B", p + "C", p + "D"),
                saved.monHoc().stream().map(MonChuongTrinhDTO::maMon).toList()); // theo học kỳ, bắt buộc trước
        assertEquals("Khoa kiểm thử", saved.tenKhoa());
        saved = programs.luuChuongTrinhKhung(admin, new ChuongTrinhKhungDTO(p + "K", null, 120, List.of(a, b, c, d)));
        assertEquals(120, saved.tongTinChi());
        assertEquals(4, saved.monHoc().size());
        assertEquals(4, programs.layChuongTrinhKhung(sv, p + "K").monHoc().size());

        // Không xóa được môn đang nằm trong chương trình khung
        expect(ErrorCode.RANG_BUOC, () -> courses.xoaMon(admin, p + "A"));

        // Dữ liệu học tập: A đạt 8.0, B rớt 3.0, C đang học, D chưa học
        jpa.inTransaction(em -> {
            for (var entry : Map.of("A", Optional.of(8.0), "B", Optional.of(3.0), "C", Optional.<Double>empty()).entrySet()) {
                MonHoc mon = em.find(MonHoc.class, p + entry.getKey());
                LopHocPhan lhp = new LopHocPhan();
                lhp.setMaLhp(p + "H" + entry.getKey());
                lhp.setMonHoc(mon);
                lhp.setHocKy(p);
                lhp.setSoLuongToiDa(10);
                em.persist(lhp);
                DangKyHocPhan dk = new DangKyHocPhan();
                dk.setId(new DangKyId(p + "S", lhp.getMaLhp()));
                dk.setSinhVien(em.find(SinhVien.class, p + "S"));
                dk.setLopHocPhan(lhp);
                dk.setNgayDangKy(LocalDateTime.now());
                dk.setDiemSo(entry.getValue().orElse(null));
                em.persist(dk);
            }
            return null;
        });
        TienDoHocTapDTO progress = programs.layTienDoCuaToi(sv);
        assertEquals(120, progress.tinChiYeuCau());
        assertEquals(3, progress.tinChiTichLuy());
        assertEquals(3, progress.tinChiDangHoc());
        assertEquals(10, progress.tinChiBatBuocYeuCau()); // A(3) + B(4) + C(3); D là tự chọn
        assertEquals(3, progress.tinChiBatBuocTichLuy());
        Map<String, TrangThaiMonKhung> status = new HashMap<>();
        progress.chuongTrinh().monHoc().forEach(m -> status.put(m.maMon(), m.trangThai()));
        assertEquals(TrangThaiMonKhung.HOAN_THANH, status.get(p + "A"));
        assertEquals(TrangThaiMonKhung.CHUA_DAT, status.get(p + "B"));
        assertEquals(TrangThaiMonKhung.DANG_HOC, status.get(p + "C"));
        assertEquals(TrangThaiMonKhung.CHUA_HOC, status.get(p + "D"));

        // Phân quyền
        expect(ErrorCode.KHONG_CO_QUYEN, () -> programs.luuChuongTrinhKhung(sv,
                new ChuongTrinhKhungDTO(p + "K", null, 10, List.of())));
        expect(ErrorCode.KHONG_CO_QUYEN, () -> programs.layTienDoCuaToi(admin));
        expect(ErrorCode.CHUA_DANG_NHAP, () -> programs.layChuongTrinhKhung("token-gia", p + "K"));

        // Xóa khoa xóa luôn chương trình khung của nó (khoa không có lớp)
        programs.luuChuongTrinhKhung(admin, new ChuongTrinhKhungDTO(p + "K2", null, 10, List.of(
                new MonChuongTrinhDTO(p + "D", null, 0, 1, false, null, null))));
        catalog.xoaKhoa(admin, p + "K2");
        expect(ErrorCode.KHONG_TIM_THAY, () -> programs.layChuongTrinhKhung(admin, p + "K2"));
    }

    private AppConfig testConfig(String semester) throws Exception {
        AppConfig source = AppConfig.load();
        Properties values = new Properties();
        Map<String, Object> jdbc = source.jdbcProperties();
        values.setProperty("db.url", (String) jdbc.get("jakarta.persistence.jdbc.url"));
        values.setProperty("db.user", (String) jdbc.get("jakarta.persistence.jdbc.user"));
        values.setProperty("db.password", (String) jdbc.get("jakarta.persistence.jdbc.password"));
        values.setProperty("rmi.hostname", "127.0.0.1");
        values.setProperty("rmi.registry.port", "1099");
        values.setProperty("rmi.export.port", "1100");
        values.setProperty("dangky.hocky", semester);
        values.setProperty("dangky.tinchi.toida", "24");
        values.setProperty("diem.qua", "4.0");
        values.setProperty("canhbao.gpa", "2.0");
        values.setProperty("dangky.retry", "3");
        Path path = temporary.resolve("server.properties");
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            values.store(writer, "Temporary settings for curriculum test");
        }
        return AppConfig.load(path);
    }

    private static void cleanup(String p, JpaUtil jpa) {
        jpa.inTransaction(em -> {
            for (String suffix : List.of("A", "B", "C")) {
                em.createQuery("delete from DangKyHocPhan d where d.id.maLhp = :l")
                        .setParameter("l", p + "H" + suffix).executeUpdate();
                LopHocPhan lhp = em.find(LopHocPhan.class, p + "H" + suffix);
                if (lhp != null) em.remove(lhp);
            }
            for (String k : List.of("K", "K2")) {
                ChuongTrinhKhung program = em.find(ChuongTrinhKhung.class, p + k);
                if (program != null) em.remove(program);
            }
            em.flush();
            for (String suffix : List.of("A", "B", "C", "D")) {
                MonHoc mon = em.find(MonHoc.class, p + suffix);
                if (mon != null) em.remove(mon);
            }
            SinhVien sv = em.find(SinhVien.class, p + "S");
            if (sv != null) em.remove(sv);
            em.flush();
            for (String id : List.of(p + "AD", p + "S")) {
                TaiKhoan account = em.find(TaiKhoan.class, id);
                if (account != null) em.remove(account);
            }
            LopHoc classroom = em.find(LopHoc.class, p + "L");
            if (classroom != null) em.remove(classroom);
            em.flush();
            for (String k : List.of("K", "K2")) {
                Khoa faculty = em.find(Khoa.class, p + k);
                if (faculty != null) em.remove(faculty);
            }
            return null;
        });
    }
}
