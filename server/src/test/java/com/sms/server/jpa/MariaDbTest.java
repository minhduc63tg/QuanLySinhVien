package com.sms.server.jpa;

import com.sms.server.config.AppConfig;
import com.sms.server.entity.Khoa;
import com.sms.server.seed.DataSeeder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class MariaDbTest {

    @Test
    void createsTenTablesSeedsOnceAndRollsBack() {
        AppConfig config = AppConfig.load();
        try (JpaUtil jpa = new JpaUtil(config)) {
            DataSeeder seeder = new DataSeeder(jpa, config.hocKyDangKy());
            boolean first = seeder.seedIfEmpty();
            List<String> tables = jpa.query(em -> em.createNativeQuery(
                    "select table_name from information_schema.tables where table_schema = database()",
                    String.class).getResultList());
            assertTrue(tables.containsAll(List.of("tai_khoan", "khoa", "lop_hoc", "sinh_vien",
                    "giang_vien", "mon_hoc", "mon_tien_quyet", "lop_hoc_phan", "lich_hoc",
                    "dang_ky_hoc_phan")));
            long accounts = jpa.query(em -> em.createQuery(
                    "select count(t) from TaiKhoan t", Long.class).getSingleResult());
            long registrations = jpa.query(em -> em.createQuery(
                    "select count(d) from DangKyHocPhan d", Long.class).getSingleResult());
            if (first) {
                assertEquals(24, accounts);
                assertEquals(56, registrations);
            }
            assertFalse(seeder.seedIfEmpty());
            assertEquals(accounts, jpa.query(em -> em.createQuery(
                    "select count(t) from TaiKhoan t", Long.class).getSingleResult()).longValue());
            assertEquals(registrations, jpa.query(em -> em.createQuery(
                    "select count(d) from DangKyHocPhan d", Long.class).getSingleResult()).longValue());
            assertEquals(0L, jpa.query(em -> em.createQuery(
                    "select count(l) from LopHocPhan l where l.soLuongDaDk <> "
                    + "(select count(d) from DangKyHocPhan d where d.lopHocPhan = l)",
                    Long.class).getSingleResult()).longValue());
            String key = UUID.randomUUID().toString().substring(0, 20);
            assertThrows(IllegalArgumentException.class, () -> jpa.inTransaction(em -> {
                Khoa khoa = new Khoa();
                khoa.setMaKhoa(key);
                khoa.setTenKhoa("Kiểm tra rollback");
                em.persist(khoa);
                em.flush();
                throw new IllegalArgumentException("rollback");
            }));
            assertNull(jpa.query(em -> em.find(Khoa.class, key)));
        }
        try (JpaUtil restarted = new JpaUtil(config)) {
            assertFalse(new DataSeeder(restarted, config.hocKyDangKy()).seedIfEmpty());
        }
    }
}
