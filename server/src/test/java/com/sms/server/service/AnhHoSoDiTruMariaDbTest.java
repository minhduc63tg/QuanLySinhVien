package com.sms.server.service;

import com.sms.common.enums.VaiTro;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.TaiKhoan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.PasswordUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Đưa ảnh hồ sơ kiểu cũ (cột du_lieu, hoặc đường dẫn tuyệt đối) về kiểu hiện tại: tệp + tên tệp trong MariaDB. */
@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class AnhHoSoDiTruMariaDbTest {

    private static final byte[] PHOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4};

    @TempDir
    Path temporary;

    private static boolean legacyColumn(JpaUtil jpa) {
        return jpa.query(em -> ((Number) em.createNativeQuery("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'anh_ho_so' and column_name = 'du_lieu'
                """).getSingleResult()).longValue() > 0);
    }

    /** Chỉ chạy khi không có dữ liệu ảnh thật cần chuyển, để test không đụng tới dữ liệu của người dùng. */
    private static void requireNoRealLegacyData(JpaUtil jpa) {
        if (legacyColumn(jpa)) {
            long blobs = jpa.query(em -> ((Number) em.createNativeQuery(
                    "select count(*) from anh_ho_so where du_lieu is not null").getSingleResult()).longValue());
            assumeTrue(blobs == 0, "Còn ảnh cũ trong cột du_lieu: hãy chạy server một lần để chuyển ra tệp");
        }
        long absolute = jpa.query(em -> ((Number) em.createNativeQuery("""
                select count(*) from anh_ho_so
                where locate('/', duong_dan) > 0 or locate(char(92), duong_dan) > 0 or locate(':', duong_dan) > 0
                """).getSingleResult()).longValue());
        assumeTrue(absolute == 0, "Còn đường dẫn tuyệt đối cũ: hãy chạy server một lần để đổi thành tên tệp");
    }

    private static void addAccount(JpaUtil jpa, String user) {
        jpa.inTransaction(em -> {
            TaiKhoan account = new TaiKhoan();
            account.setTenDangNhap(user);
            account.setMatKhau(PasswordUtil.hash("x"));
            account.setVaiTro(VaiTro.SINH_VIEN);
            em.persist(account);
            return null;
        });
    }

    private static String stored(JpaUtil jpa, String user) {
        return jpa.query(em -> em.createQuery("select a.duongDan from AnhHoSo a where a.tenDangNhap = :u",
                String.class).setParameter("u", user).getSingleResult());
    }

    private static void cleanup(JpaUtil jpa, String p) {
        jpa.inTransaction(em -> {
            em.createQuery("delete from AnhHoSo a where a.tenDangNhap like :p").setParameter("p", p + "%").executeUpdate();
            for (String suffix : new String[] {"S", "T"}) {
                TaiKhoan account = em.find(TaiKhoan.class, p + suffix);
                if (account != null) em.remove(account);
            }
            return null;
        });
    }

    @Test
    void movesLegacyBlobsToFilesAndStoresOnlyTheFileName() throws Exception {
        String p = "MG" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        try (JpaUtil jpa = new JpaUtil(AppConfig.load())) {
            requireNoRealLegacyData(jpa);
            if (!legacyColumn(jpa)) {
                jpa.inTransaction(em -> em.createNativeQuery("alter table anh_ho_so add column du_lieu longblob null")
                        .executeUpdate());
            }
            try {
                addAccount(jpa, p + "S");
                jpa.inTransaction(em -> em.createNativeQuery("""
                        insert into anh_ho_so (ten_dang_nhap, kieu_noi_dung, du_lieu, cap_nhat, duong_dan)
                        values (?1, 'image/jpeg', ?2, now(), '')
                        """).setParameter(1, p + "S").setParameter(2, PHOTO).executeUpdate());

                AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
                AnhHoSoDiTru.chay(jpa, kho);

                assertEquals(p + "S.jpg", stored(jpa, p + "S"), "MariaDB chỉ giữ tên tệp (đường dẫn tương đối)");
                assertArrayEquals(PHOTO, Files.readAllBytes(kho.thuMuc().resolve(p + "S.jpg")));
                assertFalse(legacyColumn(jpa), "cột du_lieu cũ đã được bỏ");
                AnhHoSoDiTru.chay(jpa, kho); // chạy lại không làm gì
                assertEquals(p + "S.jpg", stored(jpa, p + "S"));
            } finally {
                cleanup(jpa, p);
            }
        }
    }

    @Test
    void convertsAbsolutePathsFromThePreviousVersionIntoFileNames() throws Exception {
        String p = "MH" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        try (JpaUtil jpa = new JpaUtil(AppConfig.load())) {
            requireNoRealLegacyData(jpa);
            try {
                AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
                // S: tệp đã nằm đúng thư mục ảnh, DB giữ đường dẫn tuyệt đối
                // T: tệp nằm ở thư mục khác (thư mục ảnh đã đổi) nên phải được chép vào thư mục ảnh
                Path inside = kho.thuMuc().resolve(p + "S.jpg");
                Files.write(inside, PHOTO);
                Path elsewhere = Files.write(Files.createDirectories(temporary.resolve("thu-muc-cu")).resolve(p + "T.jpg"),
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 9, 9});
                for (var entry : java.util.Map.of(p + "S", inside, p + "T", elsewhere).entrySet()) {
                    addAccount(jpa, entry.getKey());
                    jpa.inTransaction(em -> em.createNativeQuery("""
                            insert into anh_ho_so (ten_dang_nhap, kieu_noi_dung, cap_nhat, duong_dan)
                            values (?1, 'image/jpeg', now(), ?2)
                            """).setParameter(1, entry.getKey()).setParameter(2, entry.getValue().toString()).executeUpdate());
                }

                AnhHoSoDiTru.chay(jpa, kho);

                assertEquals(p + "S.jpg", stored(jpa, p + "S"));
                assertEquals(p + "T.jpg", stored(jpa, p + "T"));
                assertArrayEquals(PHOTO, Files.readAllBytes(inside));
                assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 9, 9},
                        Files.readAllBytes(kho.thuMuc().resolve(p + "T.jpg")), "đã chép vào thư mục ảnh");
                assertTrue(Files.exists(elsewhere), "bản gốc được giữ nguyên");
                AnhHoSoDiTru.chay(jpa, kho); // chạy lại không đổi gì
                assertEquals(p + "S.jpg", stored(jpa, p + "S"));
            } finally {
                cleanup(jpa, p);
            }
        }
    }

    @Test
    void recognizesPlainFileNames() {
        assertTrue(AnhHoSoDiTru.laTenTep("SV001.jpg"));
        assertTrue(AnhHoSoDiTru.laTenTep("u7473.png"));
        assertFalse(AnhHoSoDiTru.laTenTep("D:\\a\\SV001.jpg"));
        assertFalse(AnhHoSoDiTru.laTenTep("/var/anh/SV001.jpg"));
        assertFalse(AnhHoSoDiTru.laTenTep("//MAYCHU/anh/SV001.jpg"));
        assertFalse(AnhHoSoDiTru.laTenTep("anh/SV001.jpg"));
        assertFalse(AnhHoSoDiTru.laTenTep(""));
    }
}
