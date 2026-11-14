package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.GiangVienDTO;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.GiangVienService;
import com.sms.server.entity.GiangVien;
import com.sms.server.entity.Khoa;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.HocTapMapper;
import com.sms.server.security.SessionManager;
import java.util.List;

public final class GiangVienServiceImpl extends HoSoSupport implements GiangVienService {

    private final AnhHoSoKho kho;

    public GiangVienServiceImpl(JpaUtil jpa, SessionManager sessions, AnhHoSoKho kho) {
        super(jpa, sessions);
        this.kho = kho;
    }

    @Override
    public List<GiangVienDTO> layTatCaGiangVien(String token) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        return transaction(em -> em.createQuery(
                "select g from GiangVien g left join fetch g.khoa order by g.maGv", GiangVien.class)
                .getResultList().stream().map(HocTapMapper::giangVien).toList());
    }

    @Override
    public GiangVienDTO layGiangVien(String token, String maGv) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maGv, 20, "Mã giảng viên");
        return transaction(em -> HocTapMapper.giangVien(find(em, GiangVien.class, code)));
    }

    @Override
    public GiangVienDTO layThongTinCuaToi(String token) throws BusinessException {
        var info = sessions.require(token, VaiTro.GIANG_VIEN);
        return transaction(em -> HocTapMapper.giangVien(find(em, GiangVien.class, info.tenDangNhap())));
    }

    @Override
    public GiangVienDTO themGiangVien(String token, GiangVienDTO dto) throws BusinessException {
        return save(token, dto, true);
    }

    @Override
    public GiangVienDTO suaGiangVien(String token, GiangVienDTO dto) throws BusinessException {
        return save(token, dto, false);
    }

    private GiangVienDTO save(String token, GiangVienDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Hồ sơ giảng viên không được null");
        String code = text(dto.maGv(), 20, "Mã giảng viên");
        String name = text(dto.hoTen(), 100, "Họ tên");
        String khoaCode = optional(dto.maKhoa(), 20, "Mã khoa");
        String email = email(dto.email());
        String phone = phone(dto.soDienThoai());
        return transaction(em -> {
            Khoa khoa = khoaCode == null ? null : find(em, Khoa.class, khoaCode);
            GiangVien gv;
            if (create) {
                gv = new GiangVien();
                gv.setMaGv(code);
                gv.setTaiKhoan(createAccount(em, code, VaiTro.GIANG_VIEN));
            } else {
                gv = find(em, GiangVien.class, code);
            }
            gv.setHoTen(name);
            gv.setEmail(email);
            gv.setSoDienThoai(phone);
            gv.setKhoa(khoa);
            if (create) em.persist(gv);
            return HocTapMapper.giangVien(gv);
        });
    }

    @Override
    public void xoaGiangVien(String token, String maGv) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maGv, 20, "Mã giảng viên");
        java.util.concurrent.atomic.AtomicReference<String> photoPath = new java.util.concurrent.atomic.AtomicReference<>();
        String canonical = transaction(em -> {
            GiangVien gv = find(em, GiangVien.class, code);
            noDependency(em.createQuery(
                    "select count(l) from LopHocPhan l where l.giangVien.maGv = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            noDependency(em.createQuery(
                    "select count(t) from LichThi t join t.giamThi g where g.maGv = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            var account = gv.getTaiKhoan();
            photoPath.set(em.createQuery("select a.duongDan from AnhHoSo a where a.tenDangNhap = :id", String.class)
                    .setParameter("id", account.getTenDangNhap()).getResultStream().findFirst().orElse(null));
            em.createQuery("delete from AnhHoSo a where a.tenDangNhap = :id")
                    .setParameter("id", account.getTenDangNhap()).executeUpdate();
            em.remove(gv);
            em.flush();
            em.remove(account);
            return account.getTenDangNhap();
        });
        sessions.revokeUser(canonical, null);
        kho.xoaTheoTen(photoPath.get()); // xóa luôn tệp ảnh của người dùng đã xóa
    }
}
