package com.sms.server.rmi;

import com.sms.common.rmi.RmiNames;
import com.sms.server.config.AppConfig;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.security.SessionManager;
import com.sms.server.service.*;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RmiServer implements AutoCloseable {

    private final Registry registry;
    private final List<Remote> exported = new ArrayList<>();
    private boolean closed;

    public RmiServer(AppConfig config, JpaUtil jpa, SessionManager sessions) throws RemoteException {
        this(config.registryPort(), config.exportPort(), jpa, sessions, config);
    }

    public RmiServer(int registryPort, int exportPort, JpaUtil jpa, SessionManager sessions)
            throws RemoteException {
        this(registryPort, exportPort, jpa, sessions, AppConfig.load());
    }

    public RmiServer(int registryPort, int exportPort, JpaUtil jpa, SessionManager sessions, AppConfig config)
            throws RemoteException {
        SerialFilter.install(); // phải cài trước khi nhận lời gọi RMI đầu tiên
        registry = LocateRegistry.createRegistry(registryPort);
        try {
            bind(RmiNames.AUTH_SERVICE, new AuthServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.DANH_MUC_SERVICE, new DanhMucServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.MON_HOC_SERVICE, new MonHocServiceImpl(jpa, sessions), exportPort);
            AnhHoSoKho kho = new AnhHoSoKho(config.anhThuMuc()); // thư mục tệp ảnh hồ sơ dùng chung
            bind(RmiNames.SINH_VIEN_SERVICE, new SinhVienServiceImpl(jpa, sessions, kho), exportPort);
            bind(RmiNames.GIANG_VIEN_SERVICE, new GiangVienServiceImpl(jpa, sessions, kho), exportPort);
            bind(RmiNames.LOP_HOC_PHAN_SERVICE, new LopHocPhanServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.DANG_KY_SERVICE, new DangKyServiceImpl(jpa, sessions, config), exportPort);
            bind(RmiNames.DIEM_SERVICE, new DiemServiceImpl(jpa, sessions, config), exportPort);
            bind(RmiNames.THOI_KHOA_BIEU_SERVICE, new ThoiKhoaBieuServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.THONG_KE_SERVICE, new ThongKeServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.LICH_THI_SERVICE, new LichThiServiceImpl(jpa, sessions), exportPort);
            bind(RmiNames.ANH_HO_SO_SERVICE, new AnhHoSoServiceImpl(jpa, sessions, kho), exportPort);
            bind(RmiNames.CHUONG_TRINH_KHUNG_SERVICE,
                    new ChuongTrinhKhungServiceImpl(jpa, sessions, config), exportPort);
            log.info("RMI đã sẵn sàng: registry={}, export={}", registryPort, exportPort);
        } catch (RemoteException | RuntimeException failure) {
            close();
            throw failure;
        } catch (java.io.IOException failure) { // sau RemoteException vì RemoteException là con của IOException
            close();
            throw new java.io.UncheckedIOException("Không chuẩn bị được thư mục ảnh hồ sơ", failure);
        }
    }

    private void bind(String name, Remote implementation, int port) throws RemoteException {
        Remote stub = UnicastRemoteObject.exportObject(implementation, port);
        exported.add(implementation);
        registry.rebind(name, stub);
        log.info("Đã bind service: {}", name);
    }

    Registry registryStub() throws NoSuchObjectException {
        return (Registry) UnicastRemoteObject.toStub(registry);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (Remote implementation : exported) {
            unexport(implementation);
        }
        unexport(registry);
        log.info("Đã dừng registry và các service RMI.");
    }

    private static void unexport(Remote remote) {
        try {
            UnicastRemoteObject.unexportObject(remote, true);
        } catch (NoSuchObjectException e) {
            log.debug("Đối tượng RMI đã được unexport trước đó.", e);
        }
    }
}
