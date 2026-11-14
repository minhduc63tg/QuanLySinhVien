package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.SinhVienDTO;
import com.sms.common.dto.TrangDTO;
import com.sms.common.enums.TrangThaiHocTap;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.SinhVienService;
import com.sms.server.entity.LopHoc;
import com.sms.server.entity.SinhVien;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.HocTapMapper;
import com.sms.server.security.SessionManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Locale;

public final class SinhVienServiceImpl extends HoSoSupport implements SinhVienService {

    private final AnhHoSoKho kho;

    public SinhVienServiceImpl(JpaUtil jpa, SessionManager sessions, AnhHoSoKho kho) {
        super(jpa, sessions);
        this.kho = kho;
    }

    @Override
    public SinhVienDTO themSinhVien(String token, SinhVienDTO dto) throws BusinessException {
        return save(token, dto, true);
    }

    @Override
    public SinhVienDTO suaSinhVien(String token, SinhVienDTO dto) throws BusinessException {
        return save(token, dto, false);
    }

    private SinhVienDTO save(String token, SinhVienDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Hồ sơ sinh viên không được null");
        String code = text(dto.maSv(), 20, "Mã sinh viên");
        String name = text(dto.hoTen(), 100, "Họ tên");
        String classroom = text(dto.maLop(), 20, "Mã lớp");
        String email = email(dto.email());
        String phone = phone(dto.soDienThoai());
        check(dto.ngaySinh() == null || !dto.ngaySinh().isAfter(LocalDate.now()), "Ngày sinh không hợp lệ");
        check(create || dto.trangThai() != null, "Trạng thái học tập không được null");
        return transaction(em -> {
            LopHoc lop = find(em, LopHoc.class, classroom);
            SinhVien sv;
            if (create) {
                sv = new SinhVien();
                sv.setMaSv(code);
                sv.setTaiKhoan(createAccount(em, code, VaiTro.SINH_VIEN));
            } else {
                sv = em.find(SinhVien.class, code, LockModeType.PESSIMISTIC_WRITE);
                if (sv == null) sv = find(em, SinhVien.class, code);
            }
            sv.setHoTen(name);
            sv.setNgaySinh(dto.ngaySinh());
            sv.setGioiTinh(dto.gioiTinh());
            sv.setEmail(email);
            sv.setSoDienThoai(phone);
            sv.setTrangThai(dto.trangThai() == null ? TrangThaiHocTap.DANG_HOC : dto.trangThai());
            sv.setLopHoc(lop);
            if (create) em.persist(sv);
            return HocTapMapper.sinhVien(sv);
        });
    }

    @Override
    public void xoaSinhVien(String token, String maSv) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maSv, 20, "Mã sinh viên");
        java.util.concurrent.atomic.AtomicReference<String> photoPath = new java.util.concurrent.atomic.AtomicReference<>();
        String canonical = transaction(em -> {
            SinhVien sv = em.find(SinhVien.class, code, LockModeType.PESSIMISTIC_WRITE);
            if (sv == null) sv = find(em, SinhVien.class, code);
            noDependency(em.createQuery(
                    "select count(d) from DangKyHocPhan d where d.sinhVien.maSv = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            var account = sv.getTaiKhoan();
            photoPath.set(em.createQuery("select a.duongDan from AnhHoSo a where a.tenDangNhap = :id", String.class)
                    .setParameter("id", account.getTenDangNhap()).getResultStream().findFirst().orElse(null));
            em.createQuery("delete from AnhHoSo a where a.tenDangNhap = :id")
                    .setParameter("id", account.getTenDangNhap()).executeUpdate();
            em.remove(sv);
            em.flush();
            em.remove(account);
            return account.getTenDangNhap();
        });
        sessions.revokeUser(canonical, null);
        kho.xoaTheoTen(photoPath.get()); // xóa luôn tệp ảnh của người dùng đã xóa
    }

    @Override
    public SinhVienDTO laySinhVien(String token, String maSv) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maSv, 20, "Mã sinh viên");
        return transaction(em -> HocTapMapper.sinhVien(find(em, SinhVien.class, code)));
    }

    @Override
    public SinhVienDTO layThongTinCuaToi(String token) throws BusinessException {
        var info = sessions.require(token, VaiTro.SINH_VIEN);
        return transaction(em -> HocTapMapper.sinhVien(find(em, SinhVien.class, info.tenDangNhap())));
    }

    @Override
    public void capNhatLienHe(String token, String email, String soDienThoai) throws BusinessException {
        var info = sessions.require(token, VaiTro.SINH_VIEN);
        String mail = email(email);
        String phone = phone(soDienThoai);
        transaction(em -> {
            SinhVien sv = em.find(SinhVien.class, info.tenDangNhap(), LockModeType.PESSIMISTIC_WRITE);
            if (sv == null) sv = find(em, SinhVien.class, info.tenDangNhap());
            sv.setEmail(mail);
            sv.setSoDienThoai(phone);
            return null;
        });
    }

    @Override
    public TrangDTO<SinhVienDTO> timKiem(String token, String tuKhoa, String maLop,
            TrangThaiHocTap trangThai, int trang, int kichThuoc) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(trang >= 0 && kichThuoc >= 1 && kichThuoc <= 200
                && (long) trang * kichThuoc <= Integer.MAX_VALUE, "Thông tin phân trang không hợp lệ");
        String keyword = optional(tuKhoa, 100, "Từ khóa");
        String pattern = keyword == null ? null : "%" + keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String classroom = optional(maLop, 20, "Mã lớp");
        return transaction(em -> {
            long total = em.createQuery("""
                    select count(s) from SinhVien s
                    where (:keyword is null or lower(s.maSv) like :keyword escape '!'
                        or lower(s.hoTen) like :keyword escape '!')
                    and (:classroom is null or s.lopHoc.maLop = :classroom)
                    and (:status is null or s.trangThai = :status)
                    """, Long.class).setParameter("keyword", pattern).setParameter("classroom", classroom)
                    .setParameter("status", trangThai).getSingleResult();
            var content = em.createQuery("""
                    select s from SinhVien s join fetch s.lopHoc l join fetch l.khoa
                    where (:keyword is null or lower(s.maSv) like :keyword escape '!'
                        or lower(s.hoTen) like :keyword escape '!')
                    and (:classroom is null or l.maLop = :classroom)
                    and (:status is null or s.trangThai = :status)
                    order by s.maSv
                    """, SinhVien.class).setParameter("keyword", pattern).setParameter("classroom", classroom)
                    .setParameter("status", trangThai).setFirstResult(trang * kichThuoc)
                    .setMaxResults(kichThuoc).getResultList().stream().map(HocTapMapper::sinhVien).toList();
            return new TrangDTO<>(content, total, trang, kichThuoc);
        });
    }
}
