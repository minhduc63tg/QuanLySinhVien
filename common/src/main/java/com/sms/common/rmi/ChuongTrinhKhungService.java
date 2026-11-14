package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.ChuongTrinhKhungDTO;
import com.sms.common.dto.TienDoHocTapDTO;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Mọi vai trò đã đăng nhập được xem chương trình khung của một khoa; chỉ QUAN_LY được lưu.
 * SINH_VIEN xem tiến độ của mình (khoa lấy từ lớp của sinh viên trong session).
 */
public interface ChuongTrinhKhungService extends Remote {

    /** Khoa chưa có chương trình khung thì trả về tongTinChi = 0 và danh sách môn rỗng. */
    ChuongTrinhKhungDTO layChuongTrinhKhung(String token, String maKhoa)
            throws RemoteException, BusinessException;

    /** Thay toàn bộ chương trình khung của khoa (tổng tín chỉ và danh sách môn). */
    ChuongTrinhKhungDTO luuChuongTrinhKhung(String token, ChuongTrinhKhungDTO chuongTrinh)
            throws RemoteException, BusinessException;

    TienDoHocTapDTO layTienDoCuaToi(String token)
            throws RemoteException, BusinessException;
}
