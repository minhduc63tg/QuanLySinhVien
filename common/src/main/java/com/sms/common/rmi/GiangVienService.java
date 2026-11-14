package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.GiangVienDTO;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** QUAN_LY quản lý hồ sơ; GIANG_VIEN chỉ xem hồ sơ mình qua layThongTinCuaToi. */
public interface GiangVienService extends Remote {

    List<GiangVienDTO> layTatCaGiangVien(String token)
            throws RemoteException, BusinessException;

    GiangVienDTO layGiangVien(String token, String maGv)
            throws RemoteException, BusinessException;

    GiangVienDTO themGiangVien(String token, GiangVienDTO giangVien)
            throws RemoteException, BusinessException;

    GiangVienDTO suaGiangVien(String token, GiangVienDTO giangVien)
            throws RemoteException, BusinessException;

    void xoaGiangVien(String token, String maGv)
            throws RemoteException, BusinessException;

    GiangVienDTO layThongTinCuaToi(String token)
            throws RemoteException, BusinessException;
}

