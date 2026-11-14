package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.LopHocDTO;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Mọi vai trò đã đăng nhập được đọc; chỉ QUAN_LY được thêm, sửa, xóa. */
public interface DanhMucService extends Remote {

    List<KhoaDTO> layTatCaKhoa(String token)
            throws RemoteException, BusinessException;

    KhoaDTO themKhoa(String token, KhoaDTO khoa)
            throws RemoteException, BusinessException;

    KhoaDTO suaKhoa(String token, KhoaDTO khoa)
            throws RemoteException, BusinessException;

    void xoaKhoa(String token, String maKhoa)
            throws RemoteException, BusinessException;

    List<LopHocDTO> layTatCaLop(String token)
            throws RemoteException, BusinessException;

    LopHocDTO themLop(String token, LopHocDTO lop)
            throws RemoteException, BusinessException;

    LopHocDTO suaLop(String token, LopHocDTO lop)
            throws RemoteException, BusinessException;

    void xoaLop(String token, String maLop)
            throws RemoteException, BusinessException;
}

