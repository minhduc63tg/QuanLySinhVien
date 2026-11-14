package com.sms.client.rmi;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Điểm truy cập RMI duy nhất của client. Lookup lười từng service và cache stub.
 * Khi mất kết nối: xóa cache, lookup lại và gọi lại đúng một lần; vẫn lỗi thì báo
 * "Không kết nối được máy chủ". Không được gọi trên JavaFX Application Thread.
 */
@Slf4j
public final class RmiClient {

    public static final String MSG_MAT_KET_NOI = "Không kết nối được máy chủ";

    @FunctionalInterface
    public interface RegistryFactory {
        Registry get(String host, int port) throws RemoteException;
    }

    @FunctionalInterface
    public interface Call<S, T> {
        T apply(S service) throws RemoteException, BusinessException;
    }

    private static final class Holder {
        private static final RmiClient INSTANCE = new RmiClient(ClientConfig.load(), LocateRegistry::getRegistry);
    }

    static {
        // Tránh treo lâu khi máy chủ không phản hồi
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000");
        System.setProperty("sun.rmi.transport.tcp.readTimeout", "30000");
    }

    private final ClientConfig config;
    private final RegistryFactory registries;
    private final Map<String, Remote> cache = new ConcurrentHashMap<>();

    public RmiClient(ClientConfig config, RegistryFactory registries) {
        this.config = config;
        this.registries = registries;
    }

    public static RmiClient instance() {
        return Holder.INSTANCE;
    }

    public <S extends Remote, T> T call(String name, Class<S> type, Call<S, T> call) throws BusinessException {
        try {
            return call.apply(stub(name, type));
        } catch (RemoteException | NotBoundException first) {
            log.warn("Mất kết nối tới {}, thử lại một lần: {}", name, first.toString());
            cache.clear();
            try {
                return call.apply(stub(name, type));
            } catch (RemoteException | NotBoundException second) {
                log.error("Không kết nối được máy chủ {}:{}", config.host(), config.port(), second);
                cache.clear();
                throw new BusinessException(ErrorCode.LOI_HE_THONG, MSG_MAT_KET_NOI);
            }
        }
    }

    private <S extends Remote> S stub(String name, Class<S> type) throws RemoteException, NotBoundException {
        Remote cached = cache.get(name);
        if (cached == null) {
            cached = registries.get(config.host(), config.port()).lookup(name);
            cache.put(name, cached);
        }
        return type.cast(cached);
    }
}
