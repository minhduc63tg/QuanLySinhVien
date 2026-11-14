package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.ThoiKhoaBieuDTO;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** SINH_VIEN xem lịch học của mình; GIANG_VIEN xem lịch dạy của mình, lấy mã từ session. */
public interface ThoiKhoaBieuService extends Remote {

    ThoiKhoaBieuDTO thoiKhoaBieuSinhVien(String token, String hocKy)
            throws RemoteException, BusinessException;

    ThoiKhoaBieuDTO thoiKhoaBieuGiangVien(String token, String hocKy)
            throws RemoteException, BusinessException;
}

