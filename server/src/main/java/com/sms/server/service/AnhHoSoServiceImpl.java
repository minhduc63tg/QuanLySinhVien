package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.AnhHoSoService;
import com.sms.server.entity.AnhHoSo;
import com.sms.server.entity.TaiKhoan;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * Ảnh hồ sơ lưu thành tệp trong thư mục cấu hình (anh.thumuc); MariaDB chỉ giữ tên tệp (đường dẫn tương đối
 * trong thư mục đó). Ảnh được phát cho client qua RMI nên máy nào cũng xem được, không cần truy cập thư mục ảnh.
 */
@Slf4j
public final class AnhHoSoServiceImpl extends ServiceSupport implements AnhHoSoService {

    private final AnhHoSoKho kho;

    public AnhHoSoServiceImpl(JpaUtil jpa, SessionManager sessions, AnhHoSoKho kho) {
        super(jpa, sessions);
        this.kho = kho;
    }

    @Override
    public void luuAnh(String token, String maNguoiDung, byte[] duLieu) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maNguoiDung, 20, "Mã người dùng");
        String type = AnhHoSoValidator.kiemTra(duLieu);
        Path temp;
        try {
            temp = kho.ghiTam(duLieu);
        } catch (IOException e) {
            log.error("Không ghi được tệp ảnh tạm trong {}", kho.thuMuc(), e);
            throw new BusinessException(ErrorCode.LOI_HE_THONG, "Không ghi được tệp ảnh trên máy chủ");
        }
        AtomicReference<String> oldName = new AtomicReference<>();
        String newName;
        try {
            newName = transaction(em -> {
                TaiKhoan account = layTaiKhoanCoAnh(em, code);
                Path dich = kho.duongDanDich(account.getTenDangNhap(), type);
                String name = AnhHoSoKho.tenLuu(dich);
                AnhHoSo photo = em.find(AnhHoSo.class, account.getTenDangNhap());
                boolean isNew = photo == null;
                if (isNew) {
                    photo = new AnhHoSo();
                    photo.setTaiKhoan(account);
                } else {
                    oldName.set(photo.getDuongDan());
                }
                photo.setKieuNoiDung(type);
                photo.setDuongDan(name);
                photo.setCapNhat(LocalDateTime.now());
                if (isNew) {
                    em.persist(photo);
                }
                em.flush(); // lỗi cơ sở dữ liệu lộ ra trước khi đụng tới tệp
                try {
                    kho.chuyen(temp, dich);
                } catch (IOException e) {
                    log.error("Không đặt được tệp ảnh vào {}", dich, e);
                    throw new BusinessException(ErrorCode.LOI_HE_THONG, "Không ghi được tệp ảnh trên máy chủ");
                }
                return name;
            });
        } finally {
            kho.xoaNeuCo(temp); // đã được chuyển đi thì không còn gì để xóa
        }
        // Ảnh cũ khác đuôi (ví dụ SV001.png khi giờ là SV001.jpg) không còn dùng nữa
        String previous = oldName.get();
        if (previous != null && !previous.equals(newName)) {
            kho.xoaTheoTen(previous);
        }
    }

    @Override
    public void xoaAnh(String token, String maNguoiDung) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maNguoiDung, 20, "Mã người dùng");
        String name = transaction(em -> {
            TaiKhoan account = layTaiKhoanCoAnh(em, code);
            AnhHoSo photo = em.find(AnhHoSo.class, account.getTenDangNhap());
            if (photo == null) {
                throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Người dùng chưa có ảnh hồ sơ");
            }
            String old = photo.getDuongDan();
            em.remove(photo);
            return old;
        });
        kho.xoaTheoTen(name);
    }

    @Override
    public byte[] layAnh(String token, String maNguoiDung) throws BusinessException {
        var session = sessions.require(token);
        String code = text(maNguoiDung, 20, "Mã người dùng");
        String name = transaction(em -> {
            TaiKhoan account = layTaiKhoanCoAnh(em, code);
            // Chỉ quản lý xem ảnh người khác; sinh viên/giảng viên chỉ xem ảnh của mình
            if (session.vaiTro() != VaiTro.QUAN_LY && !account.getTenDangNhap().equals(session.tenDangNhap())) {
                throw new BusinessException(ErrorCode.KHONG_CO_QUYEN, "Bạn chỉ được xem ảnh hồ sơ của chính mình");
            }
            AnhHoSo photo = em.find(AnhHoSo.class, account.getTenDangNhap());
            return photo == null ? null : photo.getDuongDan();
        });
        if (name == null) {
            return null;
        }
        try {
            return kho.doc(kho.giaiDuongDan(name));
        } catch (IOException e) {
            // Không đưa đường dẫn của máy chủ vào thông báo cho người dùng
            log.error("Không đọc được tệp ảnh '{}' của {} trong {}", name, code, kho.thuMuc(), e);
            throw new BusinessException(ErrorCode.KHONG_TIM_THAY,
                    "Không đọc được tệp ảnh trên máy chủ, hãy nhờ quản lý tải lại ảnh");
        }
    }

    @Override
    public String layDuongDan(String token, String maNguoiDung) throws BusinessException {
        sessions.require(token, VaiTro.QUAN_LY);
        String code = text(maNguoiDung, 20, "Mã người dùng");
        String name = transaction(em -> {
            TaiKhoan account = layTaiKhoanCoAnh(em, code);
            AnhHoSo photo = em.find(AnhHoSo.class, account.getTenDangNhap());
            return photo == null ? null : photo.getDuongDan();
        });
        if (name == null) {
            return null;
        }
        try {
            return kho.giaiDuongDan(name).toString();
        } catch (IOException e) {
            log.error("Tên tệp ảnh '{}' của {} không hợp lệ", name, code, e);
            throw new BusinessException(ErrorCode.LOI_HE_THONG, "Dữ liệu ảnh hồ sơ không hợp lệ");
        }
    }

    /** Chỉ sinh viên và giảng viên có ảnh hồ sơ. */
    private static TaiKhoan layTaiKhoanCoAnh(EntityManager em, String code) throws BusinessException {
        TaiKhoan account = find(em, TaiKhoan.class, code);
        if (account.getVaiTro() != VaiTro.SINH_VIEN && account.getVaiTro() != VaiTro.GIANG_VIEN) {
            throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Chỉ sinh viên và giảng viên có ảnh hồ sơ");
        }
        return account;
    }
}
