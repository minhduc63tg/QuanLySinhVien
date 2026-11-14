package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.ThongKeKhoaDTO;
import com.sms.common.dto.ThongKeTrangThaiDTO;
import com.sms.common.dto.PhanBoDiemDTO;
import com.sms.common.dto.SiSoLopHocPhanDTO;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Chỉ QUAN_LY; trả dữ liệu tổng hợp cho màn hình thống kê. */
public interface ThongKeService extends Remote {

    List<ThongKeKhoaDTO> soSinhVienTheoKhoa(String token)
            throws RemoteException, BusinessException;

    List<ThongKeTrangThaiDTO> soSinhVienTheoTrangThai(String token)
            throws RemoteException, BusinessException;

    List<PhanBoDiemDTO> phanBoDiemTheoLopHocPhan(String token, String maLhp)
            throws RemoteException, BusinessException;

    List<SiSoLopHocPhanDTO> siSoLopHocPhan(String token, String hocKy)
            throws RemoteException, BusinessException;
}

