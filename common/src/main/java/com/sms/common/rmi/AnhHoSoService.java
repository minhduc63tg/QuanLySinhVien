package com.sms.common.rmi;

import com.sms.common.BusinessException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Ảnh hồ sơ của sinh viên và giảng viên (khóa là mã người dùng = tên đăng nhập). Chỉ QUAN_LY được
 * lưu hoặc xóa; QUAN_LY xem được ảnh của mọi người, SINH_VIEN và GIANG_VIEN chỉ xem ảnh của chính mình.
 * Dữ liệu ảnh là JPEG hoặc PNG, tối đa 1 MiB và 2000x2000 điểm ảnh.
 */
public interface AnhHoSoService extends Remote {

    void luuAnh(String token, String maNguoiDung, byte[] duLieu)
            throws RemoteException, BusinessException;

    void xoaAnh(String token, String maNguoiDung)
            throws RemoteException, BusinessException;

    /** Trả về null nếu người dùng chưa có ảnh. Nội dung được server đọc từ tệp rồi gửi qua RMI. */
    byte[] layAnh(String token, String maNguoiDung)
            throws RemoteException, BusinessException;

    /**
     * Chỉ QUAN_LY. Đường dẫn tuyệt đối chính xác của tệp ảnh trên máy chủ (đúng giá trị lưu trong cơ sở dữ
     * liệu), hoặc null nếu chưa có ảnh. Máy khác mở trực tiếp được khi thư mục ảnh nằm trên ổ chia sẻ mạng.
     */
    String layDuongDan(String token, String maNguoiDung)
            throws RemoteException, BusinessException;
}
