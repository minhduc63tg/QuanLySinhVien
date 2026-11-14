package com.sms.server.seed;

import com.sms.common.enums.GioiTinh;
import com.sms.common.enums.VaiTro;
import com.sms.server.entity.*;
import com.sms.server.jpa.JpaUtil;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sms.server.security.PasswordUtil;

@Slf4j
@RequiredArgsConstructor
public final class DataSeeder {

    private static final List<String> TEN_SINH_VIEN_MAU = List.of(
            "Nguyễn Minh Khang",
            "Trần Ngọc Anh",
            "Lê Hoàng Nam",
            "Phạm Thùy Linh",
            "Hoàng Đức Huy",
            "Vũ Thanh Hà",
            "Đặng Quốc Bảo",
            "Bùi Phương Thảo",
            "Đỗ Gia Hưng",
            "Hồ Khánh Vy",
            "Ngô Tuấn Kiệt",
            "Dương Bảo Ngọc",
            "Lý Anh Tuấn",
            "Mai Quỳnh Như",
            "Trịnh Minh Quân",
            "Phan Diệu Linh",
            "Võ Thành Đạt",
            "Đinh Mỹ Duyên",
            "Lâm Nhật Minh",
            "Cao Thu Trang");

    private final JpaUtil jpa;
    private final String hocKy;

    public boolean seedIfEmpty() {
        boolean seeded = jpa.inTransaction(em -> {
            if (em.createQuery("select count(t) from TaiKhoan t", Long.class)
                    .getSingleResult() != 0) {
                return false;
            }
            taiKhoan(em, "admin", "admin123", VaiTro.QUAN_LY);
            Khoa cntt = khoa(em, "CNTT", "Công nghệ thông tin");
            Khoa kinhTe = khoa(em, "KT", "Kinh tế");
            List<LopHoc> lop = List.of(
                    lop(em, "DHKTPM22A", "Kỹ thuật phần mềm 22A", cntt),
                    lop(em, "DHCNTT22B", "Công nghệ thông tin 22B", cntt),
                    lop(em, "DHKT22A", "Kinh tế 22A", kinhTe));

            List<GiangVien> giangVien = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                String ma = "GV%03d".formatted(i);
                GiangVien gv = new GiangVien();
                gv.setTaiKhoan(taiKhoan(em, ma, ma, VaiTro.GIANG_VIEN));
                gv.setMaGv(ma);
                gv.setHoTen(List.of("Nguyễn Minh Anh", "Trần Đức Thành", "Lê Thu Hà").get(i - 1));
                gv.setEmail(ma.toLowerCase() + "@example.com");
                gv.setSoDienThoai("090000000" + i);
                gv.setKhoa(i == 3 ? kinhTe : cntt);
                em.persist(gv);
                giangVien.add(gv);
            }

            List<SinhVien> sinhVien = new ArrayList<>();
            for (int i = 1; i <= TEN_SINH_VIEN_MAU.size(); i++) {
                String ma = "SV%03d".formatted(i);
                SinhVien sv = new SinhVien();
                sv.setTaiKhoan(taiKhoan(em, ma, ma, VaiTro.SINH_VIEN));
                sv.setMaSv(ma);
                sv.setHoTen(TEN_SINH_VIEN_MAU.get(i - 1));
                sv.setNgaySinh(LocalDate.of(2004, (i - 1) % 12 + 1, (i - 1) % 28 + 1));
                sv.setGioiTinh(i % 2 == 0 ? GioiTinh.NU : GioiTinh.NAM);
                sv.setEmail(ma.toLowerCase() + "@example.com");
                sv.setSoDienThoai("091000%04d".formatted(i));
                sv.setLopHoc(lop.get((i - 1) % lop.size()));
                em.persist(sv);
                sinhVien.add(sv);
            }

            MonHoc ltcb = mon(em, "LTCB", "Lập trình cơ bản", 3);
            MonHoc oop = mon(em, "OOP", "Lập trình hướng đối tượng", 3);
            MonHoc ctdl = mon(em, "CTDL", "Cấu trúc dữ liệu và giải thuật", 3);
            MonHoc ltm = mon(em, "LTM", "Lập trình mạng", 3);
            MonHoc csdl = mon(em, "CSDL", "Cơ sở dữ liệu", 3);
            MonHoc toan = mon(em, "TOAN", "Toán cao cấp", 3);
            mon(em, "ANHVAN", "Anh văn", 2);
            mon(em, "KINHTE", "Kinh tế đại cương", 2);
            oop.getMonTienQuyet().add(ltcb);
            ctdl.getMonTienQuyet().add(oop);
            ltm.getMonTienQuyet().add(ctdl);

            String hocKyCu = hocKy.equals("HK2-2025-2026") ? "HK1-2025-2026" : "HK2-2025-2026";
            LopHocPhan cuLtcb = hocPhan(em, "LTCB-MAU-CU", ltcb, giangVien.get(0),
                    hocKyCu, 2, 1, "A101");
            LopHocPhan cuOop = hocPhan(em, "OOP-MAU-CU", oop, giangVien.get(1),
                    hocKyCu, 3, 1, "A102");
            LopHocPhan moiCtdl = hocPhan(em, "CTDL-01", ctdl, giangVien.get(0),
                    hocKy, 2, 1, "A101");
            LopHocPhan moiCsdl = hocPhan(em, "CSDL-01", csdl, giangVien.get(1),
                    hocKy, 3, 4, "A102");
            hocPhan(em, "LTM-01", ltm, giangVien.get(0), hocKy, 4, 1, "A101");
            hocPhan(em, "TOAN-01", toan, giangVien.get(2), hocKy, 5, 1, "B101");
            for (int i = 0; i < sinhVien.size(); i++) {
                dangKy(em, sinhVien.get(i), cuLtcb, i % 2 == 0 ? 8.5 : 9.0);
                dangKy(em, sinhVien.get(i), cuOop, 8.0);
                if (i < 10) {
                    dangKy(em, sinhVien.get(i), moiCtdl, null);
                }
                if (i < 6) {
                    dangKy(em, sinhVien.get(i), moiCsdl, null);
                }
            }
            return true;
        });
        if (seeded) {
            log.info("Đã tạo dữ liệu mẫu DEV: 2 khoa, 3 lớp, 8 môn, 3 giảng viên, 20 sinh viên.");
        } else {
            log.info("Đã có tài khoản; bỏ qua tạo dữ liệu mẫu.");
        }
        return seeded;
    }


    private record ThiMau(String maLhp, LocalDate ngay, int tiet, int soTiet, String phong, List<String> giamThi) {
    }

    /**
     * Tạo lịch thi mẫu DEV cho các lớp học phần mẫu của học kỳ đang mở (sáng, chiều và tối) nếu chưa có
     * lịch thi nào. Lớp hoặc giảng viên không tồn tại thì bỏ qua.
     */
    public boolean seedLichThiIfEmpty() {
        boolean seeded = jpa.inTransaction(em -> {
            if (em.createQuery("select count(t) from LichThi t", Long.class).getSingleResult() != 0) {
                return false;
            }
            boolean created = false;
            for (ThiMau mau : List.of(
                    new ThiMau("CTDL-01", LocalDate.of(2027, 1, 11), 1, 3, "A101", List.of("GV002", "GV003")),
                    new ThiMau("TOAN-01", LocalDate.of(2027, 1, 11), 6, 3, "B101", List.of("GV001")),
                    new ThiMau("CSDL-01", LocalDate.of(2027, 1, 12), 6, 3, "A102", List.of("GV001", "GV003")),
                    new ThiMau("LTM-01", LocalDate.of(2027, 1, 13), 11, 2, "B101", List.of("GV002")))) {
                LopHocPhan lhp = em.find(LopHocPhan.class, mau.maLhp());
                if (lhp == null) {
                    continue;
                }
                LichThi exam = new LichThi();
                exam.setLopHocPhan(lhp);
                exam.setNgayThi(mau.ngay());
                exam.setTietBatDau(mau.tiet());
                exam.setSoTiet(mau.soTiet());
                exam.setPhong(mau.phong());
                for (String maGv : mau.giamThi()) {
                    GiangVien gv = em.find(GiangVien.class, maGv);
                    if (gv != null) {
                        exam.getGiamThi().add(gv);
                    }
                }
                em.persist(exam);
                created = true;
            }
            return created;
        });
        if (seeded) {
            log.info("Đã tạo lịch thi mẫu DEV cho các lớp học phần mẫu.");
        }
        return seeded;
    }

    private record MonKhung(String maMon, int hocKy, boolean batBuoc) {
    }

    /**
     * Tạo chương trình khung mẫu cho khoa CNTT và KT nếu chưa có chương trình nào.
     * Tách khỏi seedIfEmpty để cơ sở dữ liệu đã seed từ trước vẫn có dữ liệu mẫu này.
     * Khoa hoặc môn không tồn tại (đã bị xóa) thì bỏ qua.
     */
    public boolean seedChuongTrinhKhungIfEmpty() {
        boolean seeded = jpa.inTransaction(em -> {
            if (em.createQuery("select count(c) from ChuongTrinhKhung c", Long.class).getSingleResult() != 0) {
                return false;
            }
            boolean created = chuongTrinhKhung(em, "CNTT", 130, List.of(
                    new MonKhung("TOAN", 1, true), new MonKhung("ANHVAN", 1, true),
                    new MonKhung("LTCB", 1, true), new MonKhung("OOP", 2, true),
                    new MonKhung("CSDL", 2, true), new MonKhung("CTDL", 3, true),
                    new MonKhung("LTM", 4, true), new MonKhung("KINHTE", 2, false)));
            created |= chuongTrinhKhung(em, "KT", 120, List.of(
                    new MonKhung("KINHTE", 1, true), new MonKhung("ANHVAN", 1, true),
                    new MonKhung("TOAN", 2, true)));
            return created;
        });
        if (seeded) {
            log.info("Đã tạo chương trình khung mẫu DEV cho khoa CNTT và KT.");
        }
        return seeded;
    }

    private static boolean chuongTrinhKhung(EntityManager em, String maKhoa, int tongTinChi, List<MonKhung> monHoc) {
        Khoa khoa = em.find(Khoa.class, maKhoa);
        if (khoa == null) {
            return false;
        }
        ChuongTrinhKhung program = new ChuongTrinhKhung();
        program.setKhoa(khoa);
        program.setTongTinChi(tongTinChi);
        for (MonKhung item : monHoc) {
            MonHoc mon = em.find(MonHoc.class, item.maMon());
            if (mon == null) {
                continue;
            }
            ChuongTrinhKhungMon row = new ChuongTrinhKhungMon();
            row.setMonHoc(mon);
            row.setHocKyGoiY(item.hocKy());
            row.setBatBuoc(item.batBuoc());
            program.themMon(row);
        }
        em.persist(program);
        return true;
    }

    private static TaiKhoan taiKhoan(EntityManager em, String ma, String password, VaiTro vaiTro) {
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(ma);
        tk.setMatKhau(PasswordUtil.hash(password));
        tk.setVaiTro(vaiTro);
        em.persist(tk);
        return tk;
    }

    private static Khoa khoa(EntityManager em, String ma, String ten) {
        Khoa khoa = new Khoa();
        khoa.setMaKhoa(ma);
        khoa.setTenKhoa(ten);
        em.persist(khoa);
        return khoa;
    }

    private static LopHoc lop(EntityManager em, String ma, String ten, Khoa khoa) {
        LopHoc lop = new LopHoc();
        lop.setMaLop(ma);
        lop.setTenLop(ten);
        lop.setKhoa(khoa);
        em.persist(lop);
        return lop;
    }

    private static MonHoc mon(EntityManager em, String ma, String ten, int tinChi) {
        MonHoc mon = new MonHoc();
        mon.setMaMon(ma);
        mon.setTenMon(ten);
        mon.setSoTinChi(tinChi);
        em.persist(mon);
        return mon;
    }

    private static LopHocPhan hocPhan(EntityManager em, String ma, MonHoc mon,
            GiangVien gv, String hocKy, int thu, int tiet, String phong) {
        LopHocPhan lhp = new LopHocPhan();
        lhp.setMaLhp(ma);
        lhp.setMonHoc(mon);
        lhp.setGiangVien(gv);
        lhp.setHocKy(hocKy);
        lhp.setSoLuongToiDa(40);
        LichHoc lich = new LichHoc();
        lich.setThu(thu);
        lich.setTietBatDau(tiet);
        lich.setSoTiet(3);
        lich.setPhong(phong);
        lhp.themLichHoc(lich);
        em.persist(lhp);
        return lhp;
    }

    private static void dangKy(EntityManager em, SinhVien sv, LopHocPhan lhp, Double diem) {
        DangKyHocPhan dk = new DangKyHocPhan();
        dk.setId(new DangKyId(sv.getMaSv(), lhp.getMaLhp()));
        dk.setSinhVien(sv);
        dk.setLopHocPhan(lhp);
        dk.setNgayDangKy(LocalDateTime.now());
        dk.setDiemSo(diem);
        em.persist(dk);
        lhp.setSoLuongDaDk(lhp.getSoLuongDaDk() + 1);
    }
}
