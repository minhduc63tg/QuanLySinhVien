package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.SessionDTO;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Đăng nhập cho mọi người dùng. resetMatKhau chỉ dành cho QUAN_LY. */
public interface AuthService extends Remote {

    SessionDTO dangNhap(String tenDangNhap, String matKhau)
            throws RemoteException, BusinessException;

    void dangXuat(String token)
            throws RemoteException, BusinessException;

    void doiMatKhau(String token, String matKhauCu, String matKhauMoi)
            throws RemoteException, BusinessException;

    void resetMatKhau(String token, String tenDangNhap)
            throws RemoteException, BusinessException;
}

