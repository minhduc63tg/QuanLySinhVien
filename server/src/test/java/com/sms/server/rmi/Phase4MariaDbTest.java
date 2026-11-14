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

@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class Phase4MariaDbTest {

    @TempDir
    Path temporary;

    @Test
    void exercisesAcademicServicesThroughRealRmiWithIsolatedFixtures() throws Exception {
        String prefix = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        AppConfig config = testConfig(prefix);
        String previousHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        try (JpaUtil jpa = new JpaUtil(config); SessionManager sessions = new SessionManager();
                RmiServer rmi = new RmiServer(0, 0, jpa, sessions, config)) {
            Fixture f = new Fixture(prefix, jpa, rmi);
            try {
                f.create();
                f.checkProfiles();
                f.checkSchedules();
                f.checkRegistrationAndGrades();
            } finally {
                f.cleanup();
            }
        } finally {
            if (previousHost == null) System.clearProperty("java.rmi.server.hostname");
            else System.setProperty("java.rmi.server.hostname", previousHost);
        }
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
        values.setProperty("dangky.tinchi.toida", "6");
        values.setProperty("diem.qua", "4.0");
        values.setProperty("canhbao.gpa", "2.0");
        values.setProperty("dangky.retry", "3");
        Path path = temporary.resolve("server.properties");
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            values.store(writer, "Temporary academic settings for integration test");
        }
        return AppConfig.load(path);
    }

    @FunctionalInterface
    private interface CheckedCall { void run() throws Exception; }

    private static void error(ErrorCode code, CheckedCall call) {
        assertEquals(code, assertThrows(BusinessException.class, call::run).getCode());
    }

    private static final class Fixture {
        private final String p;
        private final JpaUtil jpa;
        private final AuthService auth;
        private final DanhMucService catalog;
        private final MonHocService courses;
        private final SinhVienService students;
        private final GiangVienService teachers;
        private final LopHocPhanService sections;
        private final DangKyService registrations;
        private final DiemService grades;
        private final ThoiKhoaBieuService timetable;
        private final ThongKeService stats;
        private String admin;
        private String s1;
        private String s2;
        private String g1;
        private String g2;

        Fixture(String prefix, JpaUtil jpa, RmiServer rmi) throws Exception {
            p = prefix;
            this.jpa = jpa;
            var registry = rmi.registryStub();
            auth = (AuthService) registry.lookup(RmiNames.AUTH_SERVICE);
            catalog = (DanhMucService) registry.lookup(RmiNames.DANH_MUC_SERVICE);
            courses = (MonHocService) registry.lookup(RmiNames.MON_HOC_SERVICE);
            students = (SinhVienService) registry.lookup(RmiNames.SINH_VIEN_SERVICE);
            teachers = (GiangVienService) registry.lookup(RmiNames.GIANG_VIEN_SERVICE);
            sections = (LopHocPhanService) registry.lookup(RmiNames.LOP_HOC_PHAN_SERVICE);
            registrations = (DangKyService) registry.lookup(RmiNames.DANG_KY_SERVICE);
            grades = (DiemService) registry.lookup(RmiNames.DIEM_SERVICE);
            timetable = (ThoiKhoaBieuService) registry.lookup(RmiNames.THOI_KHOA_BIEU_SERVICE);
            stats = (ThongKeService) registry.lookup(RmiNames.THONG_KE_SERVICE);
            assertEquals(13, registry.list().length);
        }

        String id(String suffix) { return p + suffix; }

        void create() throws Exception {
            jpa.inTransaction(em -> {
                TaiKhoan account = new TaiKhoan();
                account.setTenDangNhap(id("AD"));
                account.setMatKhau(PasswordUtil.hash("Test@123"));
                account.setVaiTro(VaiTro.QUAN_LY);
                em.persist(account);
                return null;
            });
            admin = auth.dangNhap(id("AD"), "Test@123").token();
            catalog.themKhoa(admin, new KhoaDTO(id("K"), "Khoa kiểm thử"));
            catalog.themLop(admin, new LopHocDTO(id("L"), "Lớp kiểm thử", id("K"), null));
            for (String suffix : List.of("G1", "G2")) {
                teachers.themGiangVien(admin, new GiangVienDTO(id(suffix),
                        suffix.equals("G1") ? "Trần Minh Đức" : "Nguyễn Thu Hà", null, null, id("K"), null));
            }
            for (String suffix : List.of("S1", "S2", "S3")) {
                students.themSinhVien(admin, student(suffix, TrangThaiHocTap.DANG_HOC));
            }
            s1 = auth.dangNhap(id("S1"), id("S1")).token();
            s2 = auth.dangNhap(id("S2"), id("S2")).token();
            g1 = auth.dangNhap(id("G1"), id("G1")).token();
            g2 = auth.dangNhap(id("G2"), id("G2")).token();
            for (String suffix : List.of("BASE", "ADV", "A", "B", "C", "D")) {
                courses.themMon(admin, new MonHocDTO(id(suffix), "Môn kiểm thử " + suffix,
                        suffix.equals("B") ? 4 : 3, Set.of()));
            }
            courses.capNhatTienQuyet(admin, id("ADV"), Set.of(id("BASE")));
            sections.themLopHocPhan(admin, section("HA", "A", "G1", p, 2, 2, 1, "PA"));
            sections.themLopHocPhan(admin, section("HB", "B", "G2", p, 2, 3, 1, "PB"));
            sections.themLopHocPhan(admin, section("HC", "C", "G2", p, 2, 2, 3, "PC"));
            sections.themLopHocPhan(admin, section("FULL", "D", "G1", p, 1, 4, 1, "PD"));
            sections.themLopHocPhan(admin, section("HADV", "ADV", "G1", p, 2, 5, 1, "PE"));
            sections.themLopHocPhan(admin, section("HA2", "A", "G2", p, 2, 7, 1, "PF"));
            sections.themLopHocPhan(admin, section("OLD", "BASE", "G1", "OLD" + p, 2, 2, 1, "PA"));
        }

        SinhVienDTO student(String suffix, TrangThaiHocTap status) {
            return new SinhVienDTO(id(suffix), suffix.equals("S1") ? "Nguyễn Minh Khang" : "Trần Ngọc Anh",
                    LocalDate.of(2004, 1, 1), GioiTinh.NAM, null, null, status,
                    id("L"), null, null, null);
        }

        LopHocPhanDTO section(String suffix, String course, String teacher, String semester,
                int capacity, int day, int period, String room) {
            return new LopHocPhanDTO(id(suffix), id(course), null, 0, id(teacher), null,
                    semester, capacity, 999, 999,
                    List.of(new LichHocDTO(null, day, period, 3, id(room))));
        }

        void checkProfiles() throws Exception {
            assertEquals(id("S1"), students.layThongTinCuaToi(s1).maSv());
            assertEquals(id("G1"), teachers.layThongTinCuaToi(g1).maGv());
            assertEquals(3, students.timKiem(admin, p, id("L"), null, 0, 2).tongPhanTu());
            assertEquals(2, students.timKiem(admin, p, id("L"), null, 0, 2).noiDung().size());
            assertEquals(1, students.timKiem(admin, p, id("L"), null, 1, 2).noiDung().size());
            assertTrue(teachers.layTatCaGiangVien(admin).stream().anyMatch(g -> g.maGv().equals(id("G1"))));
            error(ErrorCode.KHONG_CO_QUYEN, () -> students.laySinhVien(s1, id("S2")));
            error(ErrorCode.KHONG_CO_QUYEN, () -> students.timKiem(g1, null, null, null, 0, 20));
            students.capNhatLienHe(s1, id("S1") + "@example.com", "0901234567");
            assertEquals(id("S1") + "@example.com", students.layThongTinCuaToi(s1).email());
            assertNull(students.layThongTinCuaToi(s2).email());
            error(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> students.capNhatLienHe(s1, "invalid", null));
            students.xoaSinhVien(admin, id("S3"));
            error(ErrorCode.SAI_THONG_TIN_DANG_NHAP, () -> auth.dangNhap(id("S3"), id("S3")));
        }

        void checkSchedules() throws Exception {
            assertEquals(6, sections.timTheoHocKy(s1, p).size());
            error(ErrorCode.TRUNG_LICH, () -> sections.luuLichHoc(admin, id("HB"),
                    List.of(new LichHocDTO(null, 2, 2, 3, id("PA")))));
            error(ErrorCode.TRUNG_LICH, () -> sections.luuLichHoc(admin, id("FULL"),
                    List.of(new LichHocDTO(null, 2, 2, 3, id("PD")))));
            sections.luuLichHoc(admin, id("HB"), List.of(new LichHocDTO(null, 3, 1, 3, id("PB"))));
            assertEquals(3, timetable.thoiKhoaBieuGiangVien(g1, p).lichHoc().size());
            error(ErrorCode.KHONG_CO_QUYEN, () -> timetable.thoiKhoaBieuGiangVien(s1, p));
        }

        void checkRegistrationAndGrades() throws Exception {
            assertEquals(p, registrations.layHocKyDangKy(s1));
            assertNull(grades.layBangDiemCuaToi(s1).gpaTichLuy());
            error(ErrorCode.KHONG_CO_QUYEN, () -> registrations.dangKy(g1, id("HA")));
            error(ErrorCode.THIEU_MON_TIEN_QUYET, () -> registrations.dangKy(s1, id("HADV")));
            error(ErrorCode.NGOAI_HOC_KY_DANG_KY, () -> registrations.dangKy(s1, id("OLD")));
            registrations.dangKy(s1, id("HA"));
            error(ErrorCode.DA_DANG_KY, () -> registrations.dangKy(s1, id("HA")));
            error(ErrorCode.DA_DANG_KY, () -> registrations.dangKy(s1, id("HA2")));
            error(ErrorCode.TRUNG_LICH, () -> registrations.dangKy(s1, id("HC")));
            error(ErrorCode.VUOT_TIN_CHI, () -> registrations.dangKy(s1, id("HB")));
            registrations.dangKy(s2, id("FULL"));
            error(ErrorCode.LOP_DAY, () -> registrations.dangKy(s1, id("FULL")));
            assertEquals(ErrorCode.LOP_DAY, registrations.layLopCoTheDangKy(s1).stream()
                    .filter(c -> c.lopHocPhan().maLhp().equals(id("FULL"))).findFirst().orElseThrow()
                    .lyDoKhongDangKy());
            registrations.huyDangKy(s2, id("FULL"));
            assertTrue(registrations.layDangKyCuaToi(s2).isEmpty());
            students.suaSinhVien(admin, student("S2", TrangThaiHocTap.TOT_NGHIEP));
            error(ErrorCode.SINH_VIEN_TOT_NGHIEP, () -> registrations.dangKy(s2, id("HA")));
            error(ErrorCode.SINH_VIEN_TOT_NGHIEP, () -> registrations.dangKy(s2, id("MISSING")));
            students.suaSinhVien(admin, student("S2", TrangThaiHocTap.DANG_HOC));

            jpa.inTransaction(em -> {
                SinhVien sv = em.find(SinhVien.class, id("S1"));
                LopHocPhan old = em.find(LopHocPhan.class, id("OLD"));
                DangKyHocPhan history = new DangKyHocPhan();
                history.setId(new DangKyId(sv.getMaSv(), old.getMaLhp()));
                history.setSinhVien(sv);
                history.setLopHocPhan(old);
                history.setNgayDangKy(LocalDateTime.now());
                history.setDiemSo(4.0);
                old.setSoLuongDaDk(1);
                em.persist(history);
                return null;
            });
            registrations.dangKy(s1, id("HADV"));
            assertEquals(2, timetable.thoiKhoaBieuSinhVien(s1, p).lichHoc().size());
            assertEquals(1, grades.layDanhSachSvTrongLop(g1, id("HA")).size());
            error(ErrorCode.KHONG_CO_QUYEN, () -> grades.layDanhSachSvTrongLop(g2, id("HA")));
            error(ErrorCode.KHONG_CO_QUYEN, () -> grades.nhapDiem(g2, id("HA"), id("S1"), 8));
            error(ErrorCode.KHONG_CO_QUYEN, () -> grades.nhapDiem(s1, id("HA"), id("S1"), 8));
            error(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> grades.nhapDiem(g1, id("HA"), id("S1"), Double.NaN));
            grades.nhapDiem(g1, id("HA"), id("S1"), 3.9);
            assertEquals(TrangThaiHocTap.CANH_BAO, students.layThongTinCuaToi(s1).trangThai());
            grades.nhapDiemHangLoat(g1, id("HA"), Map.of(id("S1"), 8.55));
            assertEquals(8.6, grades.layDanhSachSvTrongLop(g1, id("HA")).get(0).diemSo());
            assertEquals(TrangThaiHocTap.DANG_HOC, students.layThongTinCuaToi(s1).trangThai());
            assertEquals(2.4, grades.layBangDiemCuaToi(s1).gpaTichLuy(), 1e-10);
            assertEquals(3.8, grades.layBangDiemCuaToi(s1).hocKy().stream()
                    .filter(h -> h.hocKy().equals(p)).findFirst().orElseThrow().gpa(), 1e-10);
            // QUAN_LY xem được bảng điểm của sinh viên bất kỳ (để xuất PDF), kết quả giống của chính sinh viên
            assertEquals(grades.layBangDiemCuaToi(s1), grades.layBangDiem(admin, id("S1")));
            error(ErrorCode.KHONG_TIM_THAY, () -> grades.layBangDiem(admin, id("KHONGCO")));
            error(ErrorCode.KHONG_CO_QUYEN, () -> grades.layBangDiem(s1, id("S1")));
            error(ErrorCode.KHONG_CO_QUYEN, () -> grades.layBangDiem(g1, id("S1")));
            error(ErrorCode.KHONG_TIM_THAY, () -> grades.nhapDiemHangLoat(
                    g1, id("HA"), Map.of(id("S1"), 1.0, id("S2"), 2.0)));
            assertEquals(8.6, grades.layDanhSachSvTrongLop(g1, id("HA")).get(0).diemSo());
            error(ErrorCode.RANG_BUOC, () -> registrations.huyDangKy(s1, id("HA")));
            error(ErrorCode.NGOAI_HOC_KY_DANG_KY, () -> registrations.huyDangKy(s1, id("OLD")));
            assertEquals(2, stats.soSinhVienTheoKhoa(admin).stream()
                    .filter(k -> k.maKhoa().equals(id("K"))).findFirst().orElseThrow().soSinhVien());
            assertEquals(3, stats.soSinhVienTheoTrangThai(admin).size());
            assertEquals(1, stats.phanBoDiemTheoLopHocPhan(admin, id("HA")).stream()
                    .filter(d -> d.diemChu().equals("A")).findFirst().orElseThrow().soSinhVien());
            assertEquals(1, stats.siSoLopHocPhan(admin, p).stream()
                    .filter(l -> l.maLhp().equals(id("HA"))).findFirst().orElseThrow().soLuongDaDk());
            error(ErrorCode.KHONG_CO_QUYEN, () -> stats.soSinhVienTheoKhoa(s1));
            registrations.huyDangKy(s1, id("HADV"));
            assertEquals(0, stats.siSoLopHocPhan(admin, p).stream()
                    .filter(l -> l.maLhp().equals(id("HADV"))).findFirst().orElseThrow().soLuongDaDk());
            registrations.dangKy(s2, id("HA"));
            error(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> sections.suaLopHocPhan(
                    admin, section("HA", "A", "G1", p, 1, 2, 1, "PA")));
            error(ErrorCode.RANG_BUOC, () -> sections.xoaLopHocPhan(admin, id("HA")));
            error(ErrorCode.RANG_BUOC, () -> students.xoaSinhVien(admin, id("S1")));
            error(ErrorCode.RANG_BUOC, () -> teachers.xoaGiangVien(admin, id("G1")));
            students.suaSinhVien(admin, student("S1", TrangThaiHocTap.TOT_NGHIEP));
            grades.nhapDiem(admin, id("HA"), id("S1"), 1.0);
            assertEquals(TrangThaiHocTap.TOT_NGHIEP, students.layThongTinCuaToi(s1).trangThai());
        }

        void cleanup() {
            jpa.inTransaction(em -> {
                List<String> classIds = List.of(id("HA"), id("HB"), id("HC"), id("FULL"),
                        id("HADV"), id("HA2"), id("OLD"));
                em.createQuery("delete from DangKyHocPhan d where d.id.maLhp in :ids")
                        .setParameter("ids", classIds).executeUpdate();
                for (String code : classIds) {
                    LopHocPhan lhp = em.find(LopHocPhan.class, code);
                    if (lhp != null) em.remove(lhp);
                }
                em.flush();
                List<String> courseIds = List.of(id("BASE"), id("ADV"), id("A"), id("B"), id("C"), id("D"));
                for (String code : courseIds) {
                    MonHoc course = em.find(MonHoc.class, code);
                    if (course != null) course.getMonTienQuyet().clear();
                }
                em.flush();
                for (String code : courseIds) {
                    MonHoc course = em.find(MonHoc.class, code);
                    if (course != null) em.remove(course);
                }
                for (String suffix : List.of("S1", "S2", "S3")) {
                    SinhVien sv = em.find(SinhVien.class, id(suffix));
                    if (sv != null) em.remove(sv);
                }
                for (String suffix : List.of("G1", "G2")) {
                    GiangVien gv = em.find(GiangVien.class, id(suffix));
                    if (gv != null) em.remove(gv);
                }
                em.flush();
                for (String suffix : List.of("AD", "S1", "S2", "S3", "G1", "G2")) {
                    TaiKhoan account = em.find(TaiKhoan.class, id(suffix));
                    if (account != null) em.remove(account);
                }
                LopHoc classroom = em.find(LopHoc.class, id("L"));
                if (classroom != null) em.remove(classroom);
                em.flush();
                Khoa faculty = em.find(Khoa.class, id("K"));
                if (faculty != null) em.remove(faculty);
                return null;
            });
        }
    }
}
