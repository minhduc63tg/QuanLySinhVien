package com.sms.common;

import com.sms.common.dto.SessionDTO;
import com.sms.common.enums.ErrorCode;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RmiContractTest {

    @Test
    void allServicesRequireTokenAndDeclareTransportAndBusinessErrors() {
        List<Class<?>> services = List.of(AuthService.class, DanhMucService.class,
                MonHocService.class, SinhVienService.class, GiangVienService.class,
                LopHocPhanService.class, DangKyService.class, ThoiKhoaBieuService.class,
                DiemService.class, ThongKeService.class, ChuongTrinhKhungService.class, LichThiService.class,
                AnhHoSoService.class);
        for (Class<?> service : services) {
            assertTrue(Remote.class.isAssignableFrom(service));
            for (var method : service.getDeclaredMethods()) {
                List<Class<?>> exceptions = Arrays.asList(method.getExceptionTypes());
                assertTrue(exceptions.contains(RemoteException.class), method.toString());
                assertTrue(exceptions.contains(BusinessException.class), method.toString());
                assertTrue(method.getParameterCount() > 0, method.toString());
                assertEquals(String.class, method.getParameterTypes()[0], method.toString());
            }
        }
    }

    @Test
    void realRmiStubTransportsSessionAndBusinessException() throws Exception {
        String previousHost = System.getProperty("java.rmi.server.hostname");
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        AuthService implementation = new TransportFixture();
        boolean exported = false;
        try {
            AuthService stub = (AuthService) UnicastRemoteObject.exportObject(implementation, 0);
            exported = true;
            SessionDTO session = stub.dangNhap("SV001", "sample");
            assertEquals("Nguyễn Minh Khang", session.hoTen());
            assertEquals(VaiTro.SINH_VIEN, session.vaiTro());
            assertEquals("sample-token", session.token());
            BusinessException failure = assertThrows(BusinessException.class,
                    () -> stub.dangNhap("SV001", "wrong"));
            assertEquals(ErrorCode.SAI_THONG_TIN_DANG_NHAP, failure.getCode());
            assertEquals("Sai thông tin đăng nhập", failure.getMessage());
        } finally {
            if (exported) {
                UnicastRemoteObject.unexportObject(implementation, true);
            }
            if (previousHost == null) {
                System.clearProperty("java.rmi.server.hostname");
            } else {
                System.setProperty("java.rmi.server.hostname", previousHost);
            }
        }
    }

    /** Chỉ là fixture kiểm tra truyền dữ liệu qua RMI, không xử lý xác thực thực tế. */
    private static final class TransportFixture implements AuthService {
        @Override
        public SessionDTO dangNhap(String user, String password) throws BusinessException {
            if (password.equals("wrong")) {
                throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP,
                        "Sai thông tin đăng nhập");
            }
            return new SessionDTO("sample-token", user, VaiTro.SINH_VIEN, "Nguyễn Minh Khang");
        }

        @Override
        public void dangXuat(String token) {
        }

        @Override
        public void doiMatKhau(String token, String oldPassword, String newPassword) {
        }

        @Override
        public void resetMatKhau(String token, String user) {
        }
    }
}
