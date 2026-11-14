package com.sms.common.rmi;

import com.sms.common.BusinessException;
import com.sms.common.dto.LopHocPhanDTO;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.BangDiemDTO;
import java.util.List;
import java.util.Map;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** SINH_VIEN xem bảng điểm mình; GIANG_VIEN phụ trách lớp hoặc QUAN_LY xem danh sách/nhập điểm. */
public interface DiemService extends Remote {

    BangDiemDTO layBangDiemCuaToi(String token)
            throws RemoteException, BusinessException;

    /** Bảng điểm của một sinh viên bất kỳ, dùng để xuất bảng điểm; chỉ QUAN_LY. */
    BangDiemDTO layBangDiem(String token, String maSv)
            throws RemoteException, BusinessException;

    List<LopHocPhanDTO> layLopDayCuaToi(String token, String hocKy)
            throws RemoteException, BusinessException;

    List<DiemDTO> layDanhSachSvTrongLop(String token, String maLhp)
            throws RemoteException, BusinessException;

    void nhapDiem(String token, String maLhp, String maSv, double diem)
            throws RemoteException, BusinessException;

    void nhapDiemHangLoat(String token, String maLhp, Map<String, Double> diemTheoMaSv)
            throws RemoteException, BusinessException;
}

