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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Ảnh hồ sơ lưu thành tệp (MariaDB chỉ giữ đường dẫn tương đối) qua RMI thật. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class AnhHoSoMariaDbTest {

    @TempDir
    Path temporary;

    private Path anhDir;

    @FunctionalInterface
    private interface Call { void run() throws Exception; }

    private static void expect(ErrorCode code, String name, Call call) {
        BusinessException error = assertThrows(BusinessException.class, call::run, name);
        assertEquals(code, error.getCode(), name);
    }

    private static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    /** Cơ sở dữ liệu còn cột cũ du_lieu (chưa chạy server thật để chuyển ảnh ra tệp). */
    static boolean hasLegacyColumn(JpaUtil jpa) {
        return jpa.query(em -> ((Number) em.createNativeQuery("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'anh_ho_so' and column_name = 'du_lieu'
                """).getSingleResult()).longValue() > 0);
    }

    @Test
    void photosAreFilesAndDatabaseKeepsRelativePath() throws Exception {
        String p = "AH" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        anhDir = temporary.resolve("thu-muc-anh").toAbsolutePath().normalize();
        AppConfig config = testConfig(p);
        String previousHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        try (JpaUtil jpa = new JpaUtil(config); SessionManager sessions = new SessionManager()) {
            // Không tự chuyển dữ liệu cũ trong test (tránh đưa ảnh thật vào thư mục tạm): hãy chạy server thật một lần
            assumeFalse(hasLegacyColumn(jpa), "Cột du_lieu cũ còn trong bảng anh_ho_so: chạy server một lần để chuyển ảnh ra tệp");
            try (RmiServer rmi = new RmiServer(0, 0, jpa, sessions, config)) {
                try {
                    run(p, jpa, rmi);
                } finally {
                    cleanup(p, jpa);
                }
            }
        } finally {
            if (previousHost == null) System.clearProperty("java.rmi.server.hostname");
            else System.setProperty("java.rmi.server.hostname", previousHost);
        }
    }

    private List<String> filesOf(String p) throws Exception {
        try (Stream<Path> list = Files.list(anhDir)) {
            return list.map(f -> f.getFileName().toString()).filter(n -> n.startsWith(p) || n.endsWith(".part")).sorted().toList();
        }
    }

    private String storedPath(JpaUtil jpa, String user) {
        return jpa.query(em -> em.createQuery("select a.duongDan from AnhHoSo a where a.tenDangNhap = :u", String.class)
                .setParameter("u", user).getResultStream().findFirst().orElse(null));
    }

    private void run(String p, JpaUtil jpa, RmiServer rmi) throws Exception {
        var registry = rmi.registryStub();
        AuthService auth = (AuthService) registry.lookup(RmiNames.AUTH_SERVICE);
        DanhMucService catalog = (DanhMucService) registry.lookup(RmiNames.DANH_MUC_SERVICE);
        SinhVienService students = (SinhVienService) registry.lookup(RmiNames.SINH_VIEN_SERVICE);
        GiangVienService teachers = (GiangVienService) registry.lookup(RmiNames.GIANG_VIEN_SERVICE);
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
        teachers.themGiangVien(admin, new GiangVienDTO(p + "G", "Giảng viên", null, null, p + "K", null));
        for (String suffix : List.of("S1", "S2")) {
            students.themSinhVien(admin, new SinhVienDTO(p + suffix, "Sinh viên " + suffix, LocalDate.of(2004, 1, 1),
                    GioiTinh.NAM, null, null, TrangThaiHocTap.DANG_HOC, p + "L", null, null, null));
        }
        String sv1 = auth.dangNhap(p + "S1", p + "S1").token();
        String sv2 = auth.dangNhap(p + "S2", p + "S2").token();
        String gv = auth.dangNhap(p + "G", p + "G").token();

        byte[] png = image("png", 60, 80);
        byte[] jpg = image("jpg", 60, 80);

        // Chưa có ảnh: không có tệp, không có đường dẫn, ảnh là null
        assertNull(photos.layAnh(admin, p + "S1"));
        assertNull(photos.layAnh(sv1, p + "S1"));
        assertNull(photos.layDuongDan(admin, p + "S1"));
        assertTrue(Files.isDirectory(anhDir), "server tự tạo thư mục ảnh");
        assertEquals(List.of(), filesOf(p));

        // Lưu ảnh: tệp nằm đúng thư mục, DB giữ đúng đường dẫn tuyệt đối chính xác của tệp đó
        photos.luuAnh(admin, p + "S1", png);
        String expectedPng = anhDir.resolve(p + "S1.png").toString();
        String path = photos.layDuongDan(admin, p + "S1");
        assertEquals(expectedPng, path);
        assertTrue(Path.of(path).isAbsolute());
        assertEquals(p + "S1.png", storedPath(jpa, p + "S1"), "MariaDB chỉ lưu đường dẫn tương đối (tên tệp)");
        assertArrayEquals(png, Files.readAllBytes(Path.of(path)), "tệp trên đĩa");
        assertArrayEquals(png, photos.layAnh(admin, p + "S1"), "phát lại qua RMI");
        assertArrayEquals(png, photos.layAnh(sv1, p + "S1"), "chủ ảnh xem ảnh của mình");

        photos.luuAnh(admin, p + "G", jpg);
        assertEquals(anhDir.resolve(p + "G.jpg").toString(), photos.layDuongDan(admin, p + "G"));
        assertArrayEquals(jpg, photos.layAnh(gv, p + "G"));

        // Đổi sang ảnh khác đuôi: đường dẫn đổi theo, tệp cũ bị xóa, không còn tệp tạm
        photos.luuAnh(admin, p + "S1", jpg);
        String expectedJpg = anhDir.resolve(p + "S1.jpg").toString();
        assertEquals(expectedJpg, photos.layDuongDan(admin, p + "S1"));
        assertEquals(p + "S1.jpg", storedPath(jpa, p + "S1"));
        assertFalse(Files.exists(Path.of(expectedPng)), "tệp .png cũ đã bị xóa");
        assertEquals(List.of(p + "G.jpg", p + "S1.jpg"), filesOf(p));
        // Cùng đuôi: ghi đè tệp đúng tên, không sinh tệp mới
        photos.luuAnh(admin, p + "S1", image("jpg", 30, 40));
        assertEquals(List.of(p + "G.jpg", p + "S1.jpg"), filesOf(p));
        assertFalse(Arrays.equals(jpg, photos.layAnh(admin, p + "S1")));
        assertEquals(2L, jpa.query(em -> em.createQuery("select count(a) from AnhHoSo a where a.tenDangNhap like :p",
                Long.class).setParameter("p", p + "%").getSingleResult()).longValue());

        // Phân quyền: chỉ QUAN_LY lưu/xóa/xem đường dẫn; người khác không xem ảnh của nhau
        expect(ErrorCode.KHONG_CO_QUYEN, "SV lưu ảnh của mình", () -> photos.luuAnh(sv1, p + "S1", png));
        expect(ErrorCode.KHONG_CO_QUYEN, "GV lưu ảnh", () -> photos.luuAnh(gv, p + "G", png));
        expect(ErrorCode.KHONG_CO_QUYEN, "SV xóa ảnh", () -> photos.xoaAnh(sv1, p + "S1"));
        expect(ErrorCode.KHONG_CO_QUYEN, "SV xem đường dẫn của mình", () -> photos.layDuongDan(sv1, p + "S1"));
        expect(ErrorCode.KHONG_CO_QUYEN, "GV xem đường dẫn", () -> photos.layDuongDan(gv, p + "G"));
        expect(ErrorCode.KHONG_CO_QUYEN, "SV xem ảnh người khác", () -> photos.layAnh(sv2, p + "S1"));
        expect(ErrorCode.KHONG_CO_QUYEN, "SV xem ảnh giảng viên", () -> photos.layAnh(sv2, p + "G"));
        expect(ErrorCode.KHONG_CO_QUYEN, "GV xem ảnh sinh viên", () -> photos.layAnh(gv, p + "S1"));
        expect(ErrorCode.CHUA_DANG_NHAP, "token giả", () -> photos.layAnh("token-gia", p + "S1"));
        expect(ErrorCode.CHUA_DANG_NHAP, "token giả xem đường dẫn", () -> photos.layDuongDan("token-gia", p + "S1"));
        expect(ErrorCode.CHUA_DANG_NHAP, "token null", () -> photos.luuAnh(null, p + "S1", png));

        // Dữ liệu không hợp lệ: không tạo tệp, không mất ảnh cũ
        byte[] current = photos.layAnh(admin, p + "S1");
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, "không phải ảnh", () -> photos.luuAnh(admin, p + "S1", "abc".getBytes()));
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, "gif", () -> photos.luuAnh(admin, p + "S1", image("gif", 10, 10)));
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, "rỗng", () -> photos.luuAnh(admin, p + "S1", new byte[0]));
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, "null", () -> photos.luuAnh(admin, p + "S1", null));
        expect(ErrorCode.DU_LIEU_KHONG_HOP_LE, "quá lớn", () -> photos.luuAnh(admin, p + "S1", image("png", 2500, 10)));
        assertArrayEquals(current, photos.layAnh(admin, p + "S1"));
        assertEquals(List.of(p + "G.jpg", p + "S1.jpg"), filesOf(p));
        expect(ErrorCode.KHONG_TIM_THAY, "ảnh của quản lý", () -> photos.luuAnh(admin, p + "AD", png));
        expect(ErrorCode.KHONG_TIM_THAY, "tài khoản lạ", () -> photos.luuAnh(admin, p + "KHONG-CO", png));
        expect(ErrorCode.KHONG_TIM_THAY, "đường dẫn tài khoản lạ", () -> photos.layDuongDan(admin, p + "KHONG-CO"));

        // Xóa ảnh: xóa cả bản ghi lẫn tệp; xóa lần nữa báo không tìm thấy
        photos.xoaAnh(admin, p + "S1");
        assertNull(photos.layAnh(admin, p + "S1"));
        assertNull(photos.layDuongDan(admin, p + "S1"));
        assertNull(storedPath(jpa, p + "S1"));
        assertEquals(List.of(p + "G.jpg"), filesOf(p));
        expect(ErrorCode.KHONG_TIM_THAY, "xóa khi chưa có ảnh", () -> photos.xoaAnh(admin, p + "S1"));

        // Giá trị trong cơ sở dữ liệu bị sửa thành đường dẫn thoát khỏi thư mục ảnh: không đọc được tệp ngoài thư mục
        Path outside = Files.write(temporary.resolve("ngoai.jpg"), jpg);
        jpa.inTransaction(em -> em.createQuery("update AnhHoSo a set a.duongDan = :v where a.tenDangNhap = :u")
                .setParameter("v", "../ngoai.jpg").setParameter("u", p + "G").executeUpdate());
        expect(ErrorCode.KHONG_TIM_THAY, "đường dẫn tương đối thoát thư mục", () -> photos.layAnh(admin, p + "G"));
        jpa.inTransaction(em -> em.createQuery("update AnhHoSo a set a.duongDan = :v where a.tenDangNhap = :u")
                .setParameter("v", outside.toString()).setParameter("u", p + "G").executeUpdate());
        expect(ErrorCode.KHONG_TIM_THAY, "đường dẫn tuyệt đối không được nhận", () -> photos.layAnh(admin, p + "G"));
        expect(ErrorCode.LOI_HE_THONG, "dữ liệu đường dẫn hỏng", () -> photos.layDuongDan(admin, p + "G"));
        photos.luuAnh(admin, p + "G", jpg); // lưu lại là ghi đè về giá trị đúng
        assertEquals(p + "G.jpg", storedPath(jpa, p + "G"));
        assertArrayEquals(jpg, photos.layAnh(gv, p + "G"));

        // Tệp bị mất trên đĩa: báo lỗi dễ hiểu, không lộ đường dẫn của máy chủ cho người dùng
        photos.luuAnh(admin, p + "S2", png);
        Files.delete(Path.of(photos.layDuongDan(admin, p + "S2")));
        BusinessException missing = assertThrows(BusinessException.class, () -> photos.layAnh(sv2, p + "S2"));
        assertEquals(ErrorCode.KHONG_TIM_THAY, missing.getCode());
        assertFalse(missing.getMessage().contains(anhDir.toString()), "không lộ đường dẫn: " + missing.getMessage());
        assertNotNull(photos.layDuongDan(admin, p + "S2"), "quản lý vẫn thấy đường dẫn để xử lý");
        photos.luuAnh(admin, p + "S2", png); // tải lại ảnh khắc phục
        assertArrayEquals(png, photos.layAnh(sv2, p + "S2"));

        // Xóa sinh viên/giảng viên thì xóa luôn tệp ảnh (và không bị lỗi khóa ngoại)
        students.xoaSinhVien(admin, p + "S2");
        teachers.xoaGiangVien(admin, p + "G");
        assertEquals(List.of(), filesOf(p));
        assertEquals(0L, jpa.query(em -> em.createQuery("select count(a) from AnhHoSo a where a.tenDangNhap like :p",
                Long.class).setParameter("p", p + "%").getSingleResult()).longValue());
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
        values.setProperty("anh.thumuc", anhDir.toString()); // Properties.store tự thoát dấu gạch chéo ngược
        Path path = temporary.resolve("server.properties");
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            values.store(writer, "Temporary settings for photo test");
        }
        AppConfig config = AppConfig.load(path);
        assertEquals(anhDir, config.anhThuMuc(), "cấu hình anh.thumuc đọc lại đúng");
        return config;
    }

    private static void cleanup(String p, JpaUtil jpa) {
        jpa.inTransaction(em -> {
            em.createQuery("delete from AnhHoSo a where a.tenDangNhap like :p").setParameter("p", p + "%").executeUpdate();
            List<String> accounts = new ArrayList<>(List.of(p + "AD"));
            for (String suffix : List.of("S1", "S2")) {
                SinhVien sv = em.find(SinhVien.class, p + suffix);
                if (sv != null) em.remove(sv);
                accounts.add(p + suffix);
            }
            GiangVien gv = em.find(GiangVien.class, p + "G");
            if (gv != null) em.remove(gv);
            accounts.add(p + "G");
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
