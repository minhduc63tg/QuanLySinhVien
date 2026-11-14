package com.sms.common.rmi;

/** Tên bind dùng chung để client và server lookup/rebind thống nhất. */
public final class RmiNames {

    public static final int DEFAULT_REGISTRY_PORT = 1099;
    public static final int DEFAULT_EXPORT_PORT = 1100;

    public static final String AUTH_SERVICE = "sms/AuthService";
    public static final String DANH_MUC_SERVICE = "sms/DanhMucService";
    public static final String MON_HOC_SERVICE = "sms/MonHocService";
    public static final String SINH_VIEN_SERVICE = "sms/SinhVienService";
    public static final String GIANG_VIEN_SERVICE = "sms/GiangVienService";
    public static final String LOP_HOC_PHAN_SERVICE = "sms/LopHocPhanService";
    public static final String DANG_KY_SERVICE = "sms/DangKyService";
    public static final String THOI_KHOA_BIEU_SERVICE = "sms/ThoiKhoaBieuService";
    public static final String DIEM_SERVICE = "sms/DiemService";
    public static final String THONG_KE_SERVICE = "sms/ThongKeService";
    public static final String CHUONG_TRINH_KHUNG_SERVICE = "sms/ChuongTrinhKhungService";
    public static final String LICH_THI_SERVICE = "sms/LichThiService";
    public static final String ANH_HO_SO_SERVICE = "sms/AnhHoSoService";

    private RmiNames() {
    }
}

