package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichThiDTO;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * QUAN_LY xếp, xóa và xem lịch thi theo học kỳ. SINH_VIEN xem lịch thi các lớp mình đã đăng ký;
 * GIANG_VIEN xem lịch gác thi của mình (mã lấy từ session).
 */
public interface LichThiService extends Remote {

    /** Tạo hoặc thay lịch thi của lớp học phần (theo maLhp). Trùng phòng, giám thị hoặc sinh viên thì TRUNG_LICH. */
    LichThiDTO luuLichThi(String token, LichThiDTO lichThi)
            throws RemoteException, BusinessException;

    void xoaLichThi(String token, String maLhp)
            throws RemoteException, BusinessException;

    List<LichThiDTO> layLichThiTheoHocKy(String token, String hocKy)
            throws RemoteException, BusinessException;

    List<LichThiDTO> lichThiSinhVien(String token, String hocKy)
            throws RemoteException, BusinessException;

    List<LichThiDTO> lichGacThi(String token, String hocKy)
            throws RemoteException, BusinessException;
}
