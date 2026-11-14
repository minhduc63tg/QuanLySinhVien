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
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Phase 8: rà soát phân quyền bằng cách gọi từng service qua RMI thật với token sai vai trò. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class Phase8SecurityTest {

    @TempDir
    Path temporary;

    @FunctionalInterface
    private interface Call { void run() throws Exception; }

    private static void expect(ErrorCode code, String name, Call call) {
        BusinessException error = assertThrows(BusinessException.class, call::run, name);
        assertEquals(code, error.getCode(), name);
    }

    @Test
    void everyServiceEnforcesRolesAndSessions() throws Exception {
        String p = "SC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
        GiangVienService teachers = (GiangVienService) registry.lookup(RmiNames.GIANG_VIEN_SERVICE);
        LopHocPhanService sections = (LopHocPhanService) registry.lookup(RmiNames.LOP_HOC_PHAN_SERVICE);
        DangKyService registrations = (DangKyService) registry.lookup(RmiNames.DANG_KY_SERVICE);
        ThoiKhoaBieuService timetable = (ThoiKhoaBieuService) registry.lookup(RmiNames.THOI_KHOA_BIEU_SERVICE);
        DiemService grades = (DiemService) registry.lookup(RmiNames.DIEM_SERVICE);
        ThongKeService stats = (ThongKeService) registry.lookup(RmiNames.THONG_KE_SERVICE);
        ChuongTrinhKhungService programs = (ChuongTrinhKhungService) registry.lookup(RmiNames.CHUONG_TRINH_KHUNG_SERVICE);
        LichThiService exams = (LichThiService) registry.lookup(RmiNames.LICH_THI_SERVICE);
        AnhHoSoService photos = (AnhHoSoService) registry.lookup(RmiNames.ANH_HO_SO_SERVICE);

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
        catalog.themLop(admin, new LopHocDTO(p + "L", "Lớp kiểm thử", p + "K", null));
        for (String suffix : List.of("G1", "G2")) {
            teachers.themGiangVien(admin, new GiangVienDTO(p + suffix, "Giảng viên " + suffix, null, null, p + "K", null));
        }
        for (String suffix : List.of("S1", "S2", "LK")) {
            students.themSinhVien(admin, new SinhVienDTO(p + suffix, "Sinh viên " + suffix, LocalDate.of(2004, 1, 1),
                    GioiTinh.NAM, null, null, TrangThaiHocTap.DANG_HOC, p + "L", null, null, null));
        }
        courses.themMon(admin, new MonHocDTO(p + "M", "Môn kiểm thử", 3, Set.of()));
        sections.themLopHocPhan(admin, new LopHocPhanDTO(p + "H", p + "M", null, 0, p + "G1", null, p, 5, 0, 0,
                List.of(new LichHocDTO(null, 2, 1, 3, p + "P"))));
        String sv = auth.dangNhap(p + "S1", p + "S1").token();
        String gv1 = auth.dangNhap(p + "G1", p + "G1").token();
        String gv2 = auth.dangNhap(p + "G2", p + "G2").token();

        // Các lời gọi chỉ QUAN_LY được phép
        Map<String, Function<String, Call>> adminOnly = new LinkedHashMap<>();
        KhoaDTO khoa = new KhoaDTO("X", "x");
        LopHocDTO lopHoc = new LopHocDTO("X", "x", "X", null);
        MonHocDTO mon = new MonHocDTO("X", "x", 3, Set.of());
        SinhVienDTO sinhVien = new SinhVienDTO("X", "x", null, null, null, null, TrangThaiHocTap.DANG_HOC, "X", null, null, null);
        GiangVienDTO giangVien = new GiangVienDTO("X", "x", null, null, null, null);
        LopHocPhanDTO lhp = new LopHocPhanDTO("X", "X", null, 0, null, null, p, 1, 0, 0, List.of());
        adminOnly.put("themKhoa", t -> () -> catalog.themKhoa(t, khoa));
        adminOnly.put("suaKhoa", t -> () -> catalog.suaKhoa(t, khoa));
        adminOnly.put("xoaKhoa", t -> () -> catalog.xoaKhoa(t, "X"));
        adminOnly.put("themLop", t -> () -> catalog.themLop(t, lopHoc));
        adminOnly.put("suaLop", t -> () -> catalog.suaLop(t, lopHoc));
        adminOnly.put("xoaLop", t -> () -> catalog.xoaLop(t, "X"));
        adminOnly.put("themMon", t -> () -> courses.themMon(t, mon));
        adminOnly.put("suaMon", t -> () -> courses.suaMon(t, mon));
        adminOnly.put("xoaMon", t -> () -> courses.xoaMon(t, "X"));
        adminOnly.put("capNhatTienQuyet", t -> () -> courses.capNhatTienQuyet(t, "X", Set.of()));
        adminOnly.put("themSinhVien", t -> () -> students.themSinhVien(t, sinhVien));
        adminOnly.put("suaSinhVien", t -> () -> students.suaSinhVien(t, sinhVien));
        adminOnly.put("xoaSinhVien", t -> () -> students.xoaSinhVien(t, "X"));
        adminOnly.put("laySinhVien", t -> () -> students.laySinhVien(t, "X"));
        adminOnly.put("timKiem", t -> () -> students.timKiem(t, null, null, null, 0, 10));
        adminOnly.put("themGiangVien", t -> () -> teachers.themGiangVien(t, giangVien));
        adminOnly.put("suaGiangVien", t -> () -> teachers.suaGiangVien(t, giangVien));
        adminOnly.put("xoaGiangVien", t -> () -> teachers.xoaGiangVien(t, "X"));
        adminOnly.put("themLopHocPhan", t -> () -> sections.themLopHocPhan(t, lhp));
        adminOnly.put("suaLopHocPhan", t -> () -> sections.suaLopHocPhan(t, lhp));
        adminOnly.put("xoaLopHocPhan", t -> () -> sections.xoaLopHocPhan(t, "X"));
        adminOnly.put("luuLichHoc", t -> () -> sections.luuLichHoc(t, "X", List.of()));
        adminOnly.put("soSinhVienTheoKhoa", t -> () -> stats.soSinhVienTheoKhoa(t));
        adminOnly.put("soSinhVienTheoTrangThai", t -> () -> stats.soSinhVienTheoTrangThai(t));
        adminOnly.put("phanBoDiemTheoLopHocPhan", t -> () -> stats.phanBoDiemTheoLopHocPhan(t, "X"));
        adminOnly.put("siSoLopHocPhan", t -> () -> stats.siSoLopHocPhan(t, p));
        adminOnly.put("luuChuongTrinhKhung", t -> () -> programs.luuChuongTrinhKhung(t, new ChuongTrinhKhungDTO("X", "x", 0, List.of())));
        adminOnly.put("luuLichThi", t -> () -> exams.luuLichThi(t, new LichThiDTO("X", null, null, null, LocalDate.of(2027, 1, 1), 1, 1, "P", List.of(), null, 0)));
        adminOnly.put("xoaLichThi", t -> () -> exams.xoaLichThi(t, "X"));
        adminOnly.put("layLichThiTheoHocKy", t -> () -> exams.layLichThiTheoHocKy(t, p));
        adminOnly.put("luuAnh", t -> () -> photos.luuAnh(t, p + "S2", new byte[] {1, 2, 3}));
        adminOnly.put("xoaAnh", t -> () -> photos.xoaAnh(t, p + "S2"));
        adminOnly.put("layDuongDan", t -> () -> photos.layDuongDan(t, p + "S2"));
        adminOnly.put("layBangDiem", t -> () -> grades.layBangDiem(t, p + "S2"));
        adminOnly.put("resetMatKhau", t -> () -> auth.resetMatKhau(t, p + "S2"));

        // Các lời gọi chỉ SINH_VIEN được phép
        Map<String, Function<String, Call>> studentOnly = new LinkedHashMap<>();
        studentOnly.put("layHocKyDangKy", t -> () -> registrations.layHocKyDangKy(t));
        studentOnly.put("layLopCoTheDangKy", t -> () -> registrations.layLopCoTheDangKy(t));
        studentOnly.put("dangKy", t -> () -> registrations.dangKy(t, p + "H"));
        studentOnly.put("huyDangKy", t -> () -> registrations.huyDangKy(t, p + "H"));
        studentOnly.put("layDangKyCuaToi", t -> () -> registrations.layDangKyCuaToi(t));
        studentOnly.put("layBangDiemCuaToi", t -> () -> grades.layBangDiemCuaToi(t));
        studentOnly.put("sv.layThongTinCuaToi", t -> () -> students.layThongTinCuaToi(t));
        studentOnly.put("layTienDoCuaToi", t -> () -> programs.layTienDoCuaToi(t));
        studentOnly.put("lichThiSinhVien", t -> () -> exams.lichThiSinhVien(t, p));
        studentOnly.put("capNhatLienHe", t -> () -> students.capNhatLienHe(t, null, null));
        studentOnly.put("thoiKhoaBieuSinhVien", t -> () -> timetable.thoiKhoaBieuSinhVien(t, p));

        // Các lời gọi chỉ GIANG_VIEN (và QUAN_LY ở phần chấm điểm) được phép; sinh viên bị chặn
        Map<String, Function<String, Call>> teacherSide = new LinkedHashMap<>();
        teacherSide.put("gv.layThongTinCuaToi", t -> () -> teachers.layThongTinCuaToi(t));
        teacherSide.put("thoiKhoaBieuGiangVien", t -> () -> timetable.thoiKhoaBieuGiangVien(t, p));
        teacherSide.put("lichGacThi", t -> () -> exams.lichGacThi(t, p));
        teacherSide.put("layLopDayCuaToi", t -> () -> grades.layLopDayCuaToi(t, p));
        teacherSide.put("layDanhSachSvTrongLop", t -> () -> grades.layDanhSachSvTrongLop(t, p + "H"));
        teacherSide.put("nhapDiem", t -> () -> grades.nhapDiem(t, p + "H", p + "S1", 8.0));
        teacherSide.put("nhapDiemHangLoat", t -> () -> grades.nhapDiemHangLoat(t, p + "H", Map.of(p + "S1", 8.0)));

        adminOnly.forEach((name, call) -> {
            expect(ErrorCode.KHONG_CO_QUYEN, "SV gọi " + name, call.apply(sv));
            expect(ErrorCode.KHONG_CO_QUYEN, "GV gọi " + name, call.apply(gv1));
        });
        studentOnly.forEach((name, call) -> {
            expect(ErrorCode.KHONG_CO_QUYEN, "GV gọi " + name, call.apply(gv1));
            expect(ErrorCode.KHONG_CO_QUYEN, "QUAN_LY gọi " + name, call.apply(admin));
        });
        teacherSide.forEach((name, call) ->
                expect(ErrorCode.KHONG_CO_QUYEN, "SV gọi " + name, call.apply(sv)));

        // Không có phiên: token rác hoặc null đều bị yêu cầu đăng nhập
        List<Map<String, Function<String, Call>>> all = List.of(adminOnly, studentOnly, teacherSide);
        for (String bad : new String[] {"token-gia", "", null}) {
            all.forEach(group -> group.forEach((name, call) ->
                    expect(ErrorCode.CHUA_DANG_NHAP, "token '" + bad + "' gọi " + name, call.apply(bad))));
        }

        // Giảng viên không được phân công không xem/nhập điểm lớp của giảng viên khác
        expect(ErrorCode.KHONG_CO_QUYEN, "GV2 nhập điểm lớp của GV1",
                () -> grades.nhapDiem(gv2, p + "H", p + "S1", 8.0));
        expect(ErrorCode.KHONG_CO_QUYEN, "GV2 xem danh sách lớp của GV1",
                () -> grades.layDanhSachSvTrongLop(gv2, p + "H"));
        assertTrue(grades.layLopDayCuaToi(gv2, p).isEmpty());
        assertEquals(1, grades.layLopDayCuaToi(gv1, p).size());

        // Đăng xuất làm token mất hiệu lực ngay
        auth.dangXuat(sv);
        expect(ErrorCode.CHUA_DANG_NHAP, "token đã đăng xuất", () -> registrations.layHocKyDangKy(sv));

        // Đăng nhập sai 5 lần liên tiếp thì khóa, kể cả khi sau đó nhập đúng
        for (int i = 0; i < 5; i++) {
            expect(ErrorCode.SAI_THONG_TIN_DANG_NHAP, "đăng nhập sai lần " + (i + 1),
                    () -> auth.dangNhap(p + "LK", "sai-mat-khau"));
        }
        assertThrows(BusinessException.class, () -> auth.dangNhap(p + "LK", p + "LK"),
                "tài khoản phải bị khóa sau 5 lần sai");
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
            values.store(writer, "Temporary settings for security test");
        }
        return AppConfig.load(path);
    }

    private static void cleanup(String p, JpaUtil jpa) {
        jpa.inTransaction(em -> {
            LopHocPhan lhp = em.find(LopHocPhan.class, p + "H");
            if (lhp != null) em.remove(lhp);
            em.flush();
            MonHoc course = em.find(MonHoc.class, p + "M");
            if (course != null) em.remove(course);
            List<String> accounts = new ArrayList<>(List.of(p + "AD"));
            for (String suffix : List.of("S1", "S2", "LK")) {
                SinhVien sv = em.find(SinhVien.class, p + suffix);
                if (sv != null) em.remove(sv);
                accounts.add(p + suffix);
            }
            for (String suffix : List.of("G1", "G2")) {
                GiangVien gv = em.find(GiangVien.class, p + suffix);
                if (gv != null) em.remove(gv);
                accounts.add(p + suffix);
            }
            em.flush();
            for (String id : accounts) {
                TaiKhoan account = em.find(TaiKhoan.class, id);
                if (account != null) em.remove(account);
            }
            LopHoc classroom = em.find(LopHoc.class, p + "L");
            if (classroom != null) em.remove(classroom);
            em.flush();
            Khoa faculty = em.find(Khoa.class, p + "K");
            if (faculty != null) em.remove(faculty);
            return null;
        });
    }
}
