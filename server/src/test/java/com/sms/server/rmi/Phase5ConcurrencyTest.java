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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Phase 5: nhiều luồng cùng đăng ký một lớp còn ít chỗ qua RMI thật. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class Phase5ConcurrencyTest {

    private static final int STUDENTS = 30;
    private static final int SEATS = 5;
    private static final int ROUNDS = 3;

    @TempDir
    Path temporary;

    @Test
    void thirtyThreadsCompeteForFiveSeats() throws Exception {
        String p = "CC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
        LopHocPhanService sections = (LopHocPhanService) registry.lookup(RmiNames.LOP_HOC_PHAN_SERVICE);
        DangKyService registrations = (DangKyService) registry.lookup(RmiNames.DANG_KY_SERVICE);

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
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < STUDENTS; i++) {
            String id = studentId(p, i);
            students.themSinhVien(admin, new SinhVienDTO(id, "Sinh viên kiểm thử " + i,
                    LocalDate.of(2004, 1, 1), GioiTinh.NAM, null, null, TrangThaiHocTap.DANG_HOC,
                    p + "L", null, null, null));
            tokens.add(auth.dangNhap(id, id).token());
        }
        for (int round = 0; round < ROUNDS; round++) {
            courses.themMon(admin, new MonHocDTO(p + "M" + round, "Môn kiểm thử " + round, 3, Set.of()));
            // Mỗi vòng một thứ khác nhau để không trùng lịch giữa các vòng
            sections.themLopHocPhan(admin, new LopHocPhanDTO(p + "H" + round, p + "M" + round, null, 0,
                    null, null, p, SEATS, 999, 999,
                    List.of(new LichHocDTO(null, 2 + round, 1, 3, p + "P" + round))));
        }

        for (int round = 0; round < ROUNDS; round++) {
            String lhp = p + "H" + round;
            // 30 sinh viên + 5 lời gọi đăng ký trùng của 5 sinh viên đầu
            int tasks = STUDENTS + SEATS;
            ExecutorService pool = Executors.newFixedThreadPool(tasks);
            CountDownLatch ready = new CountDownLatch(tasks);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger ok = new AtomicInteger();
            Map<ErrorCode, AtomicInteger> failures = new ConcurrentHashMap<>();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                String token = tokens.get(i % STUDENTS);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        registrations.dangKy(token, lhp);
                        ok.incrementAndGet();
                    } catch (BusinessException e) {
                        failures.computeIfAbsent(e.getCode(), k -> new AtomicInteger()).incrementAndGet();
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
            pool.shutdownNow();

            long rows = jpa.query(em -> em.createQuery(
                    "select count(d) from DangKyHocPhan d where d.id.maLhp = :l", Long.class)
                    .setParameter("l", lhp).getSingleResult());
            int counter = jpa.query(em -> em.find(LopHocPhan.class, lhp).getSoLuongDaDk());
            long distinct = jpa.query(em -> em.createQuery(
                    "select count(distinct d.id.maSv) from DangKyHocPhan d where d.id.maLhp = :l", Long.class)
                    .setParameter("l", lhp).getSingleResult());
            String detail = "vòng " + round + ": thành công=" + ok.get() + ", lỗi=" + failures;
            assertEquals(SEATS, ok.get(), detail);
            assertEquals(SEATS, rows, detail);
            assertEquals(SEATS, counter, detail);
            assertEquals(rows, distinct, detail);
            // Mọi lời gọi thất bại phải mang mã lỗi nghiệp vụ hợp lệ, không phải lỗi hệ thống
            for (ErrorCode code : failures.keySet()) {
                assertTrue(code == ErrorCode.LOP_DAY || code == ErrorCode.DA_DANG_KY
                        || code == ErrorCode.XUNG_DOT_DU_LIEU, detail);
            }
        }
    }

    private static String studentId(String p, int i) {
        return p + "S" + String.format("%02d", i);
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
            values.store(writer, "Temporary settings for concurrency test");
        }
        return AppConfig.load(path);
    }

    private static void cleanup(String p, JpaUtil jpa) {
        jpa.inTransaction(em -> {
            List<String> classIds = new ArrayList<>();
            List<String> courseIds = new ArrayList<>();
            for (int r = 0; r < ROUNDS; r++) {
                classIds.add(p + "H" + r);
                courseIds.add(p + "M" + r);
            }
            em.createQuery("delete from DangKyHocPhan d where d.id.maLhp in :ids")
                    .setParameter("ids", classIds).executeUpdate();
            for (String code : classIds) {
                LopHocPhan lhp = em.find(LopHocPhan.class, code);
                if (lhp != null) em.remove(lhp);
            }
            em.flush();
            for (String code : courseIds) {
                MonHoc course = em.find(MonHoc.class, code);
                if (course != null) em.remove(course);
            }
            List<String> accountIds = new ArrayList<>(List.of(p + "AD"));
            for (int i = 0; i < STUDENTS; i++) {
                SinhVien sv = em.find(SinhVien.class, studentId(p, i));
                if (sv != null) em.remove(sv);
                accountIds.add(studentId(p, i));
            }
            em.flush();
            for (String id : accountIds) {
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
