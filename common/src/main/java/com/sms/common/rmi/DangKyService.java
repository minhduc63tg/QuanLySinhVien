package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.DangKyDTO;
import com.sms.common.dto.LopCoTheDangKyDTO;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Chỉ SINH_VIEN. Mã sinh viên luôn lấy từ session; không nhận mã sinh viên từ client. */
public interface DangKyService extends Remote {

    String layHocKyDangKy(String token)
            throws RemoteException, BusinessException;

    List<LopCoTheDangKyDTO> layLopCoTheDangKy(String token)
            throws RemoteException, BusinessException;

    void dangKy(String token, String maLhp)
            throws RemoteException, BusinessException;

    void huyDangKy(String token, String maLhp)
            throws RemoteException, BusinessException;

    List<DangKyDTO> layDangKyCuaToi(String token)
            throws RemoteException, BusinessException;
}

