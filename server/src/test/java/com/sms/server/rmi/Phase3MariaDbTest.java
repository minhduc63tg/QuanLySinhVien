package com.sms.server.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.LopHocDTO;
import com.sms.common.dto.MonHocDTO;
import com.sms.common.dto.SessionDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.AuthService;
import com.sms.common.rmi.DanhMucService;
import com.sms.common.rmi.MonHocService;
import com.sms.common.rmi.RmiNames;
import com.sms.server.config.AppConfig;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.PasswordUtil;
import com.sms.server.security.SessionManager;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "sms.db.test", matches = "true")
class Phase3MariaDbTest {

    @Test
    void realServicesAuthenticateEnforceRolesAndRejectPrerequisiteCycles() throws Exception {
        String previousHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        try (JpaUtil jpa = new JpaUtil(AppConfig.load());
                SessionManager sessions = new SessionManager();
                RmiServer rmi = new RmiServer(0, 0, jpa, sessions)) {
            Fixture fixture = new Fixture(jpa);
            try {
                fixture.createAccountsAndProfile();
                AuthService auth = (AuthService) rmi.registryStub().lookup(RmiNames.AUTH_SERVICE);
                DanhMucService catalog = (DanhMucService) rmi.registryStub().lookup(RmiNames.DANH_MUC_SERVICE);
                MonHocService courses = (MonHocService) rmi.registryStub().lookup(RmiNames.MON_HOC_SERVICE);
                assertEquals(13, rmi.registryStub().list().length);

                error(ErrorCode.SAI_THONG_TIN_DANG_NHAP, () -> auth.dangNhap(fixture.student, "wrong"));
                SessionDTO admin = auth.dangNhap(fixture.admin, "Test@123");
                SessionDTO student = auth.dangNhap(fixture.student, "Test@123");
                assertEquals(VaiTro.QUAN_LY, admin.vaiTro());
                assertEquals(VaiTro.SINH_VIEN, student.vaiTro());
                assertEquals("Nguyễn Minh Khang", student.hoTen());
                error(ErrorCode.CHUA_DANG_NHAP, () -> catalog.layTatCaKhoa("invalid"));
                error(ErrorCode.KHONG_CO_QUYEN, () -> catalog.themKhoa(student.token(), null));
                error(ErrorCode.KHONG_CO_QUYEN, () -> courses.capNhatTienQuyet(
                        student.token(), fixture.a, Set.of()));
                error(ErrorCode.KHONG_CO_QUYEN, () -> auth.resetMatKhau(student.token(), fixture.admin));

                String token = admin.token();
                String extraKhoa = fixture.prefix + "K2";
                catalog.themKhoa(token, new KhoaDTO(extraKhoa, "Khoa kiểm thử"));
                error(ErrorCode.DA_TON_TAI,
                        () -> catalog.themKhoa(token, new KhoaDTO(extraKhoa, "Trùng mã")));
                assertEquals("Khoa đã sửa",
                        catalog.suaKhoa(token, new KhoaDTO(extraKhoa, "Khoa đã sửa")).tenKhoa());
                LopHocDTO updated = catalog.suaLop(token, new LopHocDTO(
                        fixture.lop, "Lớp đã sửa", extraKhoa, "Tên giả từ client"));
                assertEquals("Khoa đã sửa", updated.tenKhoa());
                assertTrue(catalog.layTatCaLop(student.token()).stream()
                        .anyMatch(lop -> lop.maLop().equals(fixture.lop)));
                error(ErrorCode.RANG_BUOC, () -> catalog.xoaKhoa(token, extraKhoa));
                error(ErrorCode.RANG_BUOC, () -> catalog.xoaLop(token, fixture.lop));

                for (String code : List.of(fixture.a, fixture.b, fixture.c)) {
                    courses.themMon(token, new MonHocDTO(code, "Môn kiểm thử " + code, 3, Set.of()));
                }
                assertEquals(4, courses.suaMon(token,
                        new MonHocDTO(fixture.c, "Môn đã sửa", 4, Set.of())).soTinChi());
                courses.capNhatTienQuyet(token, fixture.b, Set.of(fixture.a));
                courses.capNhatTienQuyet(token, fixture.c, Set.of(fixture.b));
                error(ErrorCode.DU_LIEU_KHONG_HOP_LE,
                        () -> courses.capNhatTienQuyet(token, fixture.a, Set.of(fixture.c)));
                error(ErrorCode.DU_LIEU_KHONG_HOP_LE,
                        () -> courses.capNhatTienQuyet(token, fixture.a, Set.of(fixture.a)));
                assertTrue(courses.layMon(token, fixture.a).maMonTienQuyet().isEmpty());
                assertEquals(fixture.b, courses.layTienQuyet(token, fixture.c).get(0).maMon());
                assertTrue(courses.layTatCaMon(student.token()).stream()
                        .anyMatch(mon -> mon.maMon().equals(fixture.c)));
                error(ErrorCode.RANG_BUOC, () -> courses.xoaMon(token, fixture.a));
                courses.capNhatTienQuyet(token, fixture.c, Set.of());
                courses.xoaMon(token, fixture.c);
                error(ErrorCode.KHONG_TIM_THAY, () -> courses.layMon(token, fixture.c));

                SessionDTO other = auth.dangNhap(fixture.student, "Test@123");
                error(ErrorCode.SAI_THONG_TIN_DANG_NHAP,
                        () -> auth.doiMatKhau(student.token(), "wrong", "Changed@123"));
                error(ErrorCode.DU_LIEU_KHONG_HOP_LE,
                        () -> auth.doiMatKhau(student.token(), "Test@123", "short"));
                auth.doiMatKhau(student.token(), "Test@123", "Changed@123");
                error(ErrorCode.CHUA_DANG_NHAP, () -> catalog.layTatCaKhoa(other.token()));
                assertFalse(catalog.layTatCaKhoa(student.token()).isEmpty());
                error(ErrorCode.SAI_THONG_TIN_DANG_NHAP,
                        () -> auth.dangNhap(fixture.student, "Test@123"));
                SessionDTO changed = auth.dangNhap(fixture.student, "Changed@123");
                auth.resetMatKhau(token, fixture.student);
                error(ErrorCode.CHUA_DANG_NHAP, () -> catalog.layTatCaKhoa(changed.token()));
                SessionDTO reset = auth.dangNhap(fixture.student, fixture.student);
                auth.dangXuat(reset.token());
                error(ErrorCode.CHUA_DANG_NHAP, () -> catalog.layTatCaKhoa(reset.token()));
            } finally {
                fixture.cleanup();
            }
        } finally {
            if (previousHost == null) {
                System.clearProperty("java.rmi.server.hostname");
            } else {
                System.setProperty("java.rmi.server.hostname", previousHost);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedCall {
        void run() throws Exception;
    }

    private static void error(ErrorCode code, CheckedCall work) {
        assertEquals(code, assertThrows(BusinessException.class, work::run).getCode());
    }

    private static final class Fixture {
        private final JpaUtil jpa;
        private final String prefix = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        private final String admin = prefix + "AD";
        private final String student = prefix + "SV";
        private final String khoa = prefix + "K";
        private final String lop = prefix + "L";
        private final String a = prefix + "A";
        private final String b = prefix + "B";
        private final String c = prefix + "C";

        private Fixture(JpaUtil jpa) {
            this.jpa = jpa;
        }

        private void createAccountsAndProfile() {
            jpa.inTransaction(em -> {
                TaiKhoan manager = new TaiKhoan();
                manager.setTenDangNhap(admin);
                manager.setMatKhau(PasswordUtil.hash("Test@123"));
                manager.setVaiTro(VaiTro.QUAN_LY);
                em.persist(manager);
                TaiKhoan account = new TaiKhoan();
                account.setTenDangNhap(student);
                account.setMatKhau(PasswordUtil.hash("Test@123"));
                account.setVaiTro(VaiTro.SINH_VIEN);
                em.persist(account);
                Khoa faculty = new Khoa();
                faculty.setMaKhoa(khoa);
                faculty.setTenKhoa("Khoa kiểm thử");
                em.persist(faculty);
                LopHoc classroom = new LopHoc();
                classroom.setMaLop(lop);
                classroom.setTenLop("Lớp kiểm thử");
                classroom.setKhoa(faculty);
                em.persist(classroom);
                SinhVien sv = new SinhVien();
                sv.setMaSv(student);
                sv.setTaiKhoan(account);
                sv.setHoTen("Nguyễn Minh Khang");
                sv.setLopHoc(classroom);
                em.persist(sv);
                return null;
            });
        }

        private void cleanup() {
            jpa.inTransaction(em -> {
                for (String code : List.of(a, b, c)) {
                    MonHoc mon = em.find(MonHoc.class, code);
                    if (mon != null) {
                        mon.getMonTienQuyet().clear();
                    }
                }
                em.flush();
                for (String code : List.of(a, b, c)) {
                    MonHoc mon = em.find(MonHoc.class, code);
                    if (mon != null) {
                        em.remove(mon);
                    }
                }
                SinhVien sv = em.find(SinhVien.class, student);
                if (sv != null) {
                    em.remove(sv);
                }
                em.flush();
                for (String user : List.of(student, admin)) {
                    TaiKhoan account = em.find(TaiKhoan.class, user);
                    if (account != null) {
                        em.remove(account);
                    }
                }
                LopHoc classroom = em.find(LopHoc.class, lop);
                if (classroom != null) {
                    em.remove(classroom);
                }
                em.flush();
                for (String code : List.of(khoa, prefix + "K2")) {
                    Khoa faculty = em.find(Khoa.class, code);
                    if (faculty != null) {
                        em.remove(faculty);
                    }
                }
                return null;
            });
        }
    }
}
