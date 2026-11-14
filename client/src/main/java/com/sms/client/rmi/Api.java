package com.sms.client.rmi;

import com.sms.client.session.SessionHolder;
import com.sms.common.BusinessException;
import com.sms.common.rmi.*;

/**
 * Lối tắt gọi từng service RMI. Dùng bên trong AsyncRunner, ví dụ:
 * {@code AsyncRunner.run(() -> Api.danhMuc(s -> s.layTatCaKhoa(Api.token())), ...)}.
 */
public final class Api {

    private Api() {
    }

    public static String token() {
        return SessionHolder.token();
    }

    public static <T> T auth(RmiClient.Call<AuthService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.AUTH_SERVICE, AuthService.class, call);
    }

    public static <T> T danhMuc(RmiClient.Call<DanhMucService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.DANH_MUC_SERVICE, DanhMucService.class, call);
    }

    public static <T> T monHoc(RmiClient.Call<MonHocService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.MON_HOC_SERVICE, MonHocService.class, call);
    }

    public static <T> T sinhVien(RmiClient.Call<SinhVienService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.SINH_VIEN_SERVICE, SinhVienService.class, call);
    }

    public static <T> T giangVien(RmiClient.Call<GiangVienService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.GIANG_VIEN_SERVICE, GiangVienService.class, call);
    }

    public static <T> T lopHocPhan(RmiClient.Call<LopHocPhanService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.LOP_HOC_PHAN_SERVICE, LopHocPhanService.class, call);
    }

    public static <T> T dangKy(RmiClient.Call<DangKyService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.DANG_KY_SERVICE, DangKyService.class, call);
    }

    public static <T> T thoiKhoaBieu(RmiClient.Call<ThoiKhoaBieuService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.THOI_KHOA_BIEU_SERVICE, ThoiKhoaBieuService.class, call);
    }

    public static <T> T diem(RmiClient.Call<DiemService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.DIEM_SERVICE, DiemService.class, call);
    }

    public static <T> T thongKe(RmiClient.Call<ThongKeService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.THONG_KE_SERVICE, ThongKeService.class, call);
    }

    public static <T> T chuongTrinhKhung(RmiClient.Call<ChuongTrinhKhungService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.CHUONG_TRINH_KHUNG_SERVICE, ChuongTrinhKhungService.class, call);
    }

    public static <T> T lichThi(RmiClient.Call<LichThiService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.LICH_THI_SERVICE, LichThiService.class, call);
    }

    public static <T> T anhHoSo(RmiClient.Call<AnhHoSoService, T> call) throws BusinessException {
        return RmiClient.instance().call(RmiNames.ANH_HO_SO_SERVICE, AnhHoSoService.class, call);
    }

    public static ClientConfig config() {
        return Holder.CONFIG;
    }

    private static final class Holder {
        private static final ClientConfig CONFIG = ClientConfig.load();
    }
}
