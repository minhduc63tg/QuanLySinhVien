package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.LichHocDTO;
import com.sms.common.dto.LopHocPhanDTO;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** QUAN_LY quản lý lớp và lịch; mọi vai trò đã đăng nhập được tìm lớp theo học kỳ. */
public interface LopHocPhanService extends Remote {

    LopHocPhanDTO themLopHocPhan(String token, LopHocPhanDTO lopHocPhan)
            throws RemoteException, BusinessException;

    LopHocPhanDTO suaLopHocPhan(String token, LopHocPhanDTO lopHocPhan)
            throws RemoteException, BusinessException;

    void xoaLopHocPhan(String token, String maLhp)
            throws RemoteException, BusinessException;

    void luuLichHoc(String token, String maLhp, List<LichHocDTO> lichHoc)
            throws RemoteException, BusinessException;

    List<LopHocPhanDTO> timTheoHocKy(String token, String hocKy)
            throws RemoteException, BusinessException;
}

