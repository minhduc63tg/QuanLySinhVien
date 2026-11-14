package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.SinhVienDTO;
import com.sms.common.dto.TrangDTO;
import com.sms.common.enums.TrangThaiHocTap;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** QUAN_LY quản lý hồ sơ và tìm kiếm. SINH_VIEN chỉ xem mình/cập nhật liên hệ qua session. */
public interface SinhVienService extends Remote {

    SinhVienDTO themSinhVien(String token, SinhVienDTO sinhVien)
            throws RemoteException, BusinessException;

    SinhVienDTO suaSinhVien(String token, SinhVienDTO sinhVien)
            throws RemoteException, BusinessException;

    void xoaSinhVien(String token, String maSv)
            throws RemoteException, BusinessException;

    SinhVienDTO laySinhVien(String token, String maSv)
            throws RemoteException, BusinessException;

    TrangDTO<SinhVienDTO> timKiem(String token, String tuKhoa, String maLop,
            TrangThaiHocTap trangThai, int trang, int kichThuoc)
            throws RemoteException, BusinessException;

    SinhVienDTO layThongTinCuaToi(String token)
            throws RemoteException, BusinessException;

    void capNhatLienHe(String token, String email, String soDienThoai)
            throws RemoteException, BusinessException;
}

