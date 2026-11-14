package com.sms.client.rmi;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import com.sms.common.rmi.AuthService;
import java.lang.reflect.Proxy;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RmiClientTest {

    private static final AuthService STUB = (AuthService) Proxy.newProxyInstance(
            RmiClientTest.class.getClassLoader(), new Class<?>[] {AuthService.class},
            (proxy, method, args) -> null);

    private static final class FakeRegistry implements Registry {
        final AtomicInteger lookups = new AtomicInteger();

        @Override public Remote lookup(String name) { lookups.incrementAndGet(); return STUB; }
        @Override public void bind(String name, Remote obj) throws AlreadyBoundException { }
        @Override public void unbind(String name) throws NotBoundException { }
        @Override public void rebind(String name, Remote obj) { }
        @Override public String[] list() { return new String[0]; }
    }

    private static RmiClient client(FakeRegistry registry) {
        return new RmiClient(new ClientConfig("localhost", 1099, "HK1"), (host, port) -> registry);
    }

    @Test
    void cachesStubBetweenCalls() throws Exception {
        FakeRegistry registry = new FakeRegistry();
        RmiClient client = client(registry);
        client.call("x", AuthService.class, s -> "a");
        client.call("x", AuthService.class, s -> "b");
        assertEquals(1, registry.lookups.get());
    }

    @Test
    void retriesOnceAfterConnectionLoss() throws Exception {
        FakeRegistry registry = new FakeRegistry();
        AtomicInteger attempts = new AtomicInteger();
        String result = client(registry).call("x", AuthService.class, s -> {
            if (attempts.incrementAndGet() == 1) throw new RemoteException("down");
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(2, registry.lookups.get());
    }

    @Test
    void reportsFriendlyMessageWhenStillDown() {
        AtomicInteger attempts = new AtomicInteger();
        BusinessException error = assertThrows(BusinessException.class,
                () -> client(new FakeRegistry()).call("x", AuthService.class, s -> {
                    attempts.incrementAndGet();
                    throw new RemoteException("down");
                }));
        assertEquals(2, attempts.get());
        assertEquals(RmiClient.MSG_MAT_KET_NOI, error.getMessage());
    }

    @Test
    void businessErrorIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        BusinessException error = assertThrows(BusinessException.class,
                () -> client(new FakeRegistry()).call("x", AuthService.class, s -> {
                    attempts.incrementAndGet();
                    throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP, "Sai");
                }));
        assertEquals(1, attempts.get());
        assertEquals(ErrorCode.SAI_THONG_TIN_DANG_NHAP, error.getCode());
    }
}
