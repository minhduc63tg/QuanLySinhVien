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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Mã lớp học phần tự sinh, lịch thi và lịch gác thi qua RMI thật với MariaDB. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class LichThiMariaDbTest {

    private static final LocalDate NGAY = LocalDate.of(2027, 2, 1);

    @TempDir
    Path temporary;

    @FunctionalInterface
    private interface Call { void run() throws Exception; }

    private static void expect(ErrorCode code, String name, Call call) {
        assertEquals(code, assertThrows(BusinessException.class, call::run, name).getCode(), name);
    }

    private static void expect(ErrorCode code, Call call) {
        assertEquals(code, assertThrows(BusinessException.class, call::run).getCode());
    }

    @Test
    void generatesClassCodesAndManagesExamSchedules() throws Exception {
        String p = "LT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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

    private static LichThiDTO exam(String lhp, LocalDate day, int tiet, int soTiet, String phong, String... giamThi) {
        return new LichThiDTO(lhp, null, null, null, day, tiet, soTiet, phong, List.of(giamThi), null, 0);
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
        LichThiService exams = (LichThiService) registry.lookup(RmiNames.LICH_THI_SERVICE);

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
        for (String suffix : List.of("G1", "G2", "G3")) {
            teachers.themGiangVien(admin, new GiangVienDTO(p + suffix, "Giảng viên " + suffix, null, null, p + "K", null));
        }
        for (String suffix : List.of("S1", "S2")) {
            students.themSinhVien(admin, new SinhVienDTO(p + suffix, "Sinh viên " + suffix, LocalDate.of(2004, 1, 1),
                    GioiTinh.NAM, null, null, TrangThaiHocTap.DANG_HOC, p + "L", null, null, null));
        }
        for (String suffix : List.of("A", "B", "C")) {
            courses.themMon(admin, new MonHocDTO(p + suffix, "Môn " + suffix, 3, Set.of()));
        }

        // ---- Mã lớp học phần tự sinh: mã môn + số thứ tự 2 chữ số theo số lớp đã mở trong học kỳ
        String semester = p;
        String otherSemester = p + "X";
        LopHocPhanDTO first = sections.themLopHocPhan(admin, lhp(null, p + "A", semester, 1, "P1"));
        LopHocPhanDTO second = sections.themLopHocPhan(admin, lhp("  ", p + "A", semester, 2, "P1"));
        assertEquals(p + "A01", first.maLhp());
        assertEquals(p + "A02", second.maLhp());
        // Môn khác bắt đầu lại từ 01
        LopHocPhanDTO b1 = sections.themLopHocPhan(admin, lhp(null, p + "B", semester, 3, "P1"));
        assertEquals(p + "B01", b1.maLhp());
        // Học kỳ khác: số 01, 02 đã được dùng nên lấy số trống kế tiếp
        LopHocPhanDTO later = sections.themLopHocPhan(admin, lhp(null, p + "A", otherSemester, 1, "P1"));
        assertEquals(p + "A03", later.maLhp());
        // Vẫn cho phép chỉ định mã khi gọi trực tiếp API; mã trùng bị từ chối
        assertEquals(p + "MANUAL", sections.themLopHocPhan(admin, lhp(p + "MANUAL", p + "C", semester, 4, "P1")).maLhp());
        expect(ErrorCode.DA_TON_TAI, () -> sections.themLopHocPhan(admin, lhp(p + "MANUAL", p + "C", semester, 5, "P1")));
        // Sửa lớp thì bắt buộc có mã
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> sections.suaLopHocPhan(admin, lhp(null, p + "A", semester, 1, "P1")));

        String a1 = first.maLhp();
        String a2 = second.maLhp();
        String bb = b1.maLhp();
        String manual = p + "MANUAL";

        // ---- Lịch thi
        String sv1 = auth.dangNhap(p + "S1", p + "S1").token();
        String sv2 = auth.dangNhap(p + "S2", p + "S2").token();
        String gv1 = auth.dangNhap(p + "G1", p + "G1").token();
        String gv3 = auth.dangNhap(p + "G3", p + "G3").token();
        // S1 học A01 và B01; S2 học A02
        jpa.inTransaction(em -> {
            enroll(em, p + "S1", a1);
            enroll(em, p + "S1", bb);
            enroll(em, p + "S2", a2);
            return null;
        });

        LichThiDTO saved = exams.luuLichThi(admin, exam(a1, NGAY, 1, 3, "T101", p + "G1", p + "G2"));
        assertEquals(NGAY, saved.ngayThi());
        assertEquals(List.of(p + "G1", p + "G2"), saved.maGiamThi());
        assertEquals(List.of("Giảng viên G1", "Giảng viên G2"), saved.tenGiamThi());
        assertEquals("Môn A", saved.tenMon());
        assertEquals(1, saved.soThiSinh());

        // Trùng phòng cùng giờ / giám thị / sinh viên
        expect(ErrorCode.TRUNG_LICH, "phòng", () -> exams.luuLichThi(admin, exam(a2, NGAY, 3, 2, "t101", p + "G3")));
        expect(ErrorCode.TRUNG_LICH, "giám thị", () -> exams.luuLichThi(admin, exam(a2, NGAY, 2, 2, "T102", p + "G2")));
        expect(ErrorCode.TRUNG_LICH, "sinh viên", () -> exams.luuLichThi(admin, exam(bb, NGAY, 3, 2, "T102", p + "G3")));
        // Cùng phòng nhưng không giao tiết, hoặc khác ngày thì hợp lệ
        exams.luuLichThi(admin, exam(a2, NGAY, 4, 2, "T101", p + "G3"));
        exams.luuLichThi(admin, exam(bb, NGAY.plusDays(1), 1, 3, "T101", p + "G1"));
        // Giám thị không tồn tại, dữ liệu sai
        expect(ErrorCode.KHONG_TIM_THAY, () -> exams.luuLichThi(admin, exam(manual, NGAY, 8, 2, "T9", p + "KHONG-CO")));
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, () -> exams.luuLichThi(admin, exam(manual, NGAY, 11, 3, "T9")));
        expect(ErrorCode.KHONG_TIM_THAY, () -> exams.luuLichThi(admin, exam(p + "KHONG-CO", NGAY, 1, 1, "T9")));

        // Lưu lại là thay lịch thi cũ (không tạo thêm), và không tự trùng với chính nó
        LichThiDTO moved = exams.luuLichThi(admin, exam(a1, NGAY, 1, 2, "T101", p + "G1"));
        assertEquals(2, moved.soTiet());
        assertEquals(List.of(p + "G1"), moved.maGiamThi());
        assertEquals(3, exams.layLichThiTheoHocKy(admin, semester).size());

        // Sinh viên chỉ thấy lịch thi các lớp mình đã đăng ký; giảng viên chỉ thấy lịch gác của mình
        assertEquals(List.of(a1, bb), exams.lichThiSinhVien(sv1, semester).stream().map(LichThiDTO::maLhp).toList());
        assertEquals(List.of(a2), exams.lichThiSinhVien(sv2, semester).stream().map(LichThiDTO::maLhp).toList());
        assertEquals(List.of(a1, bb), exams.lichGacThi(gv1, semester).stream().map(LichThiDTO::maLhp).toList());
        assertEquals(List.of(a2), exams.lichGacThi(gv3, semester).stream().map(LichThiDTO::maLhp).toList());
        assertTrue(exams.lichThiSinhVien(sv1, otherSemester).isEmpty());

        // Phân quyền
        expect(ErrorCode.KHONG_CO_QUYEN, () -> exams.luuLichThi(sv1, exam(a1, NGAY, 1, 1, "T9")));
        expect(ErrorCode.KHONG_CO_QUYEN, () -> exams.xoaLichThi(gv1, a1));
        expect(ErrorCode.KHONG_CO_QUYEN, () -> exams.layLichThiTheoHocKy(sv1, semester));
        expect(ErrorCode.KHONG_CO_QUYEN, () -> exams.lichThiSinhVien(gv1, semester));
        expect(ErrorCode.KHONG_CO_QUYEN, () -> exams.lichGacThi(sv1, semester));
        expect(ErrorCode.CHUA_DANG_NHAP, () -> exams.lichGacThi("token-gia", semester));

        // Ràng buộc: không xóa giảng viên đang gác thi; xóa lớp thì xóa luôn lịch thi
        expect(ErrorCode.RANG_BUOC, () -> teachers.xoaGiangVien(admin, p + "G3"));
        exams.xoaLichThi(admin, a2);
        expect(ErrorCode.KHONG_TIM_THAY, () -> exams.xoaLichThi(admin, a2));
        teachers.xoaGiangVien(admin, p + "G3");
        jpa.inTransaction(em -> {
            em.createQuery("delete from DangKyHocPhan d where d.id.maLhp = :l").setParameter("l", bb).executeUpdate();
            return null;
        });
        sections.xoaLopHocPhan(admin, bb);
        assertEquals(1, exams.layLichThiTheoHocKy(admin, semester).size());
        assertEquals(0L, jpa.query(em -> em.createQuery("select count(t) from LichThi t where t.lopHocPhan.maLhp = :l",
                Long.class).setParameter("l", bb).getSingleResult()).longValue());
    }

    private static LopHocPhanDTO lhp(String code, String course, String semester, int day, String room) {
        return new LopHocPhanDTO(code, course, null, 0, null, null, semester, 40, 0, 0,
                List.of(new LichHocDTO(null, day + 1, 1, 3, room + semester)));
    }

    private static void enroll(jakarta.persistence.EntityManager em, String maSv, String maLhp) {
        LopHocPhan lhp = em.find(LopHocPhan.class, maLhp);
        DangKyHocPhan dk = new DangKyHocPhan();
        dk.setId(new DangKyId(maSv, maLhp));
        dk.setSinhVien(em.find(SinhVien.class, maSv));
        dk.setLopHocPhan(lhp);
        dk.setNgayDangKy(java.time.LocalDateTime.now());
        em.persist(dk);
        lhp.setSoLuongDaDk(lhp.getSoLuongDaDk() + 1);
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
            values.store(writer, "Temporary settings for exam schedule test");
        }
        return AppConfig.load(path);
    }

    private static void cleanup(String p, JpaUtil jpa) {
        jpa.inTransaction(em -> {
            List<LopHocPhan> classes = em.createQuery("select l from LopHocPhan l where l.maLhp like :prefix",
                    LopHocPhan.class).setParameter("prefix", p + "%").getResultList();
            for (LopHocPhan lhp : classes) {
                em.createQuery("select t from LichThi t where t.lopHocPhan.maLhp = :c", LichThi.class)
                        .setParameter("c", lhp.getMaLhp()).getResultList().forEach(em::remove);
                em.createQuery("delete from DangKyHocPhan d where d.id.maLhp = :c")
                        .setParameter("c", lhp.getMaLhp()).executeUpdate();
            }
            em.flush();
            classes.forEach(em::remove);
            em.flush();
            for (String suffix : List.of("A", "B", "C")) {
                MonHoc mon = em.find(MonHoc.class, p + suffix);
                if (mon != null) em.remove(mon);
            }
            List<String> accounts = new ArrayList<>(List.of(p + "AD"));
            for (String suffix : List.of("S1", "S2")) {
                SinhVien sv = em.find(SinhVien.class, p + suffix);
                if (sv != null) em.remove(sv);
                accounts.add(p + suffix);
            }
            for (String suffix : List.of("G1", "G2", "G3")) {
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
