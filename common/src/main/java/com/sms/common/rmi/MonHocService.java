package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.MonHocDTO;
import java.util.List;
import java.util.Set;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Mọi vai trò đã đăng nhập được đọc; QUAN_LY quản lý môn và quan hệ tiên quyết. */
public interface MonHocService extends Remote {

    List<MonHocDTO> layTatCaMon(String token)
            throws RemoteException, BusinessException;

    MonHocDTO layMon(String token, String maMon)
            throws RemoteException, BusinessException;

    MonHocDTO themMon(String token, MonHocDTO mon)
            throws RemoteException, BusinessException;

    MonHocDTO suaMon(String token, MonHocDTO mon)
            throws RemoteException, BusinessException;

    void xoaMon(String token, String maMon)
            throws RemoteException, BusinessException;

    void capNhatTienQuyet(String token, String maMon, Set<String> maMonTienQuyet)
            throws RemoteException, BusinessException;

    List<MonHocDTO> layTienQuyet(String token, String maMon)
            throws RemoteException, BusinessException;
}

