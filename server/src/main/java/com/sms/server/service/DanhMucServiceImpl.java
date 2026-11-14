package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.LopHocDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.DanhMucService;
import com.sms.server.entity.ChuongTrinhKhung;
import com.sms.server.entity.Khoa;
import com.sms.server.entity.LopHoc;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.DanhMucMapper;
import com.sms.server.security.SessionManager;
import java.util.List;

public final class DanhMucServiceImpl extends ServiceSupport implements DanhMucService {

    public DanhMucServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public List<KhoaDTO> layTatCaKhoa(String token) throws BusinessException {
        sessions.require(token);
        return transaction(em -> em.createQuery("select k from Khoa k order by k.maKhoa", Khoa.class)
                .getResultStream().map(DanhMucMapper::khoa).toList());
    }

    @Override
    public KhoaDTO themKhoa(String token, KhoaDTO dto) throws BusinessException {
        return luuKhoa(token, dto, true);
    }

    @Override
    public KhoaDTO suaKhoa(String token, KhoaDTO dto) throws BusinessException {
        return luuKhoa(token, dto, false);
    }

    private KhoaDTO luuKhoa(String token, KhoaDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Dữ liệu khoa không được trống");
        String code = text(dto.maKhoa(), 20, "Mã khoa");
        String name = text(dto.tenKhoa(), 100, "Tên khoa");
        return transaction(em -> {
            Khoa khoa = em.find(Khoa.class, code);
            if (create && khoa != null) {
                throw new BusinessException(ErrorCode.DA_TON_TAI, "Mã khoa đã tồn tại");
            }
            if (!create && khoa == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy khoa");
            }
            if (create) {
                khoa = new Khoa();
                khoa.setMaKhoa(code);
            }
            khoa.setTenKhoa(name);
            if (create) {
                em.persist(khoa);
            }
            return DanhMucMapper.khoa(khoa);
        });
    }

    @Override
    public void xoaKhoa(String token, String maKhoa) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maKhoa, 20, "Mã khoa");
        transaction(em -> {
            Khoa khoa = find(em, Khoa.class, code);
            noDependency(em.createQuery("select count(l) from LopHoc l where l.khoa.maKhoa = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            noDependency(em.createQuery("select count(g) from GiangVien g where g.khoa.maKhoa = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            // Chương trình khung thuộc về khoa nên xóa cùng khoa
            ChuongTrinhKhung program = em.find(ChuongTrinhKhung.class, code);
            if (program != null) {
                em.remove(program);
                em.flush();
            }
            em.remove(khoa);
            return null;
        });
    }

    @Override
    public List<LopHocDTO> layTatCaLop(String token) throws BusinessException {
        sessions.require(token);
        return transaction(em -> em.createQuery(
                "select l from LopHoc l join fetch l.khoa order by l.maLop", LopHoc.class)
                .getResultStream().map(DanhMucMapper::lop).toList());
    }

    @Override
    public LopHocDTO themLop(String token, LopHocDTO dto) throws BusinessException {
        return luuLop(token, dto, true);
    }

    @Override
    public LopHocDTO suaLop(String token, LopHocDTO dto) throws BusinessException {
        return luuLop(token, dto, false);
    }

    private LopHocDTO luuLop(String token, LopHocDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Dữ liệu lớp không được trống");
        String code = text(dto.maLop(), 20, "Mã lớp");
        String name = text(dto.tenLop(), 100, "Tên lớp");
        String khoaCode = text(dto.maKhoa(), 20, "Mã khoa");
        return transaction(em -> {
            Khoa khoa = find(em, Khoa.class, khoaCode);
            LopHoc lop = em.find(LopHoc.class, code);
            if (create && lop != null) {
                throw new BusinessException(ErrorCode.DA_TON_TAI, "Mã lớp đã tồn tại");
            }
            if (!create && lop == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy lớp");
            }
            if (create) {
                lop = new LopHoc();
                lop.setMaLop(code);
            }
            lop.setTenLop(name);
            lop.setKhoa(khoa);
            if (create) {
                em.persist(lop);
            }
            return DanhMucMapper.lop(lop);
        });
    }

    @Override
    public void xoaLop(String token, String maLop) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maLop, 20, "Mã lớp");
        transaction(em -> {
            LopHoc lop = find(em, LopHoc.class, code);
            noDependency(em.createQuery("select count(s) from SinhVien s where s.lopHoc.maLop = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            em.remove(lop);
            return null;
        });
    }
}
