package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.dto.MonHocDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.MonHocService;
import com.sms.server.entity.MonHoc;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.mapper.DanhMucMapper;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MonHocServiceImpl extends ServiceSupport implements MonHocService {

    public MonHocServiceImpl(JpaUtil jpa, SessionManager sessions) {
        super(jpa, sessions);
    }

    @Override
    public List<MonHocDTO> layTatCaMon(String token) throws BusinessException {
        sessions.require(token);
        return transaction(em -> fetchAll(em).stream().map(DanhMucMapper::mon).toList());
    }

    @Override
    public MonHocDTO layMon(String token, String maMon) throws BusinessException {
        sessions.require(token);
        String code = text(maMon, 20, "Mã môn");
        return transaction(em -> DanhMucMapper.mon(find(em, MonHoc.class, code)));
    }

    @Override
    public MonHocDTO themMon(String token, MonHocDTO dto) throws BusinessException {
        return luuMon(token, dto, true);
    }

    @Override
    public MonHocDTO suaMon(String token, MonHocDTO dto) throws BusinessException {
        return luuMon(token, dto, false);
    }

    private MonHocDTO luuMon(String token, MonHocDTO dto, boolean create) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        check(dto != null, "Dữ liệu môn học không được trống");
        String code = text(dto.maMon(), 20, "Mã môn");
        String name = text(dto.tenMon(), 150, "Tên môn");
        check(dto.soTinChi() >= 1 && dto.soTinChi() <= 10, "Số tín chỉ phải nằm trong [1, 10]");
        return transaction(em -> {
            MonHoc mon = em.find(MonHoc.class, code);
            if (create && mon != null) {
                throw new BusinessException(ErrorCode.DA_TON_TAI, "Mã môn đã tồn tại");
            }
            if (!create && mon == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy môn");
            }
            if (create) {
                mon = new MonHoc();
                mon.setMaMon(code);
            }
            mon.setTenMon(name);
            mon.setSoTinChi(dto.soTinChi());
            if (create) {
                em.persist(mon);
            }
            return DanhMucMapper.mon(mon);
        });
    }

    @Override
    public void xoaMon(String token, String maMon) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maMon, 20, "Mã môn");
        transaction(em -> {
            lockCatalog(em);
            MonHoc mon = find(em, MonHoc.class, code);
            noDependency(em.createQuery(
                    "select count(l) from LopHocPhan l where l.monHoc.maMon = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            noDependency(em.createQuery(
                    "select count(m) from MonHoc m join m.monTienQuyet p where p.maMon = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            noDependency(em.createQuery(
                    "select count(c) from ChuongTrinhKhungMon c where c.monHoc.maMon = :code", Long.class)
                    .setParameter("code", code).getSingleResult());
            em.remove(mon);
            return null;
        });
    }

    @Override
    public void capNhatTienQuyet(String token, String maMon, Set<String> maMonTienQuyet)
            throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maMon, 20, "Mã môn");
        check(maMonTienQuyet != null, "Danh sách môn tiên quyết không được null");
        Set<String> prerequisites = new HashSet<>();
        for (String prerequisite : maMonTienQuyet) {
            prerequisites.add(text(prerequisite, 20, "Mã môn tiên quyết"));
        }
        transaction(em -> {
            // Khóa cùng thứ tự để hai yêu cầu đồng thời không tạo vòng chéo A->B và B->A.
            lockCatalog(em);
            Map<String, MonHoc> entities = new HashMap<>();
            Map<String, Set<String>> graph = new HashMap<>();
            for (MonHoc mon : fetchAll(em)) {
                entities.put(mon.getMaMon(), mon);
                graph.put(mon.getMaMon(), mon.getMonTienQuyet().stream()
                        .map(MonHoc::getMaMon).collect(Collectors.toSet()));
            }
            TienQuyetValidator.validate(code, prerequisites, graph);
            MonHoc mon = entities.get(code);
            mon.getMonTienQuyet().clear();
            for (String prerequisite : prerequisites) {
                mon.getMonTienQuyet().add(entities.get(prerequisite));
            }
            return null;
        });
    }

    @Override
    public List<MonHocDTO> layTienQuyet(String token, String maMon) throws BusinessException {
        sessions.require(token);
        String code = text(maMon, 20, "Mã môn");
        return transaction(em -> {
            Map<String, MonHoc> all = fetchAll(em).stream()
                    .collect(Collectors.toMap(MonHoc::getMaMon, mon -> mon));
            MonHoc mon = all.get(code);
            if (mon == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy môn");
            }
            return mon.getMonTienQuyet().stream()
                    .sorted(java.util.Comparator.comparing(MonHoc::getMaMon))
                    .map(DanhMucMapper::mon).toList();
        });
    }

    private static List<MonHoc> fetchAll(EntityManager em) {
        return em.createQuery(
                "select distinct m from MonHoc m left join fetch m.monTienQuyet order by m.maMon",
                MonHoc.class).getResultList();
    }

    private static void lockCatalog(EntityManager em) {
        em.createQuery("select m from MonHoc m order by m.maMon", MonHoc.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
}
