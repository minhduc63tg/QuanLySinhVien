package com.sms.server.mapper;

import com.sms.common.dto.*;
import com.sms.server.entity.*;
import com.sms.server.service.GpaCalculator;
import java.util.Comparator;

public final class HocTapMapper {

    private HocTapMapper() {
    }

    public static SinhVienDTO sinhVien(SinhVien s) {
        LopHoc l = s.getLopHoc();
        return new SinhVienDTO(s.getMaSv(), s.getHoTen(), s.getNgaySinh(), s.getGioiTinh(),
                s.getEmail(), s.getSoDienThoai(), s.getTrangThai(), l.getMaLop(), l.getTenLop(),
                l.getKhoa().getMaKhoa(), l.getKhoa().getTenKhoa());
    }

    public static GiangVienDTO giangVien(GiangVien g) {
        Khoa k = g.getKhoa();
        return new GiangVienDTO(g.getMaGv(), g.getHoTen(), g.getEmail(), g.getSoDienThoai(),
                k == null ? null : k.getMaKhoa(), k == null ? null : k.getTenKhoa());
    }

    public static LichHocDTO lich(LichHoc l) {
        return new LichHocDTO(l.getId(), l.getThu(), l.getTietBatDau(), l.getSoTiet(), l.getPhong());
    }

    public static LopHocPhanDTO lop(LopHocPhan l) {
        GiangVien g = l.getGiangVien();
        MonHoc m = l.getMonHoc();
        return new LopHocPhanDTO(l.getMaLhp(), m.getMaMon(), m.getTenMon(), m.getSoTinChi(),
                g == null ? null : g.getMaGv(), g == null ? null : g.getHoTen(),
                l.getHocKy(), l.getSoLuongToiDa(), l.getSoLuongDaDk(), l.getVersion(),
                l.getLichHoc().stream().sorted(Comparator.comparingInt(LichHoc::getThu)
                        .thenComparingInt(LichHoc::getTietBatDau)).map(HocTapMapper::lich).toList());
    }

    public static DangKyDTO dangKy(DangKyHocPhan d) {
        return new DangKyDTO(lop(d.getLopHocPhan()), d.getNgayDangKy(), d.getDiemSo());
    }

    public static DiemDTO diem(DangKyHocPhan d) {
        MonHoc m = d.getLopHocPhan().getMonHoc();
        var grade = d.getDiemSo() == null ? null : GpaCalculator.quyDoi(d.getDiemSo());
        return new DiemDTO(d.getSinhVien().getMaSv(), d.getSinhVien().getHoTen(),
                d.getLopHocPhan().getMaLhp(), m.getMaMon(), m.getTenMon(), m.getSoTinChi(),
                d.getLopHocPhan().getHocKy(), d.getDiemSo(), grade == null ? null : grade.diemChu(),
                grade == null ? null : grade.diemHe4(), grade == null ? null : grade.dat());
    }

    public static GpaCalculator.DiemMon diemMon(DangKyHocPhan d) {
        MonHoc m = d.getLopHocPhan().getMonHoc();
        return new GpaCalculator.DiemMon(m.getMaMon(), m.getSoTinChi(), d.getDiemSo());
    }

    public static TietHocDTO tiet(LopHocPhan l, LichHoc slot) {
        return new TietHocDTO(l.getMaLhp(), l.getMonHoc().getMaMon(), l.getMonHoc().getTenMon(),
                l.getGiangVien() == null ? null : l.getGiangVien().getMaGv(),
                l.getGiangVien() == null ? null : l.getGiangVien().getHoTen(),
                slot.getThu(), slot.getTietBatDau(), slot.getSoTiet(), slot.getPhong());
    }
}
