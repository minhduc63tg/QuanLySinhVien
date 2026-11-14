package com.sms.server.rmi;

import com.sms.common.dto.SinhVienDTO;
import com.sms.common.enums.TrangThaiHocTap;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Status;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SerialFilterTest {

    private static Status check(Class<?> type) {
        return check(type, -1);
    }

    private static Status check(Class<?> type, long arrayLength) {
        ObjectInputFilter filter = SerialFilter.create();
        return filter.checkInput(new ObjectInputFilter.FilterInfo() {
            @Override public Class<?> serialClass() { return type; }
            @Override public long arrayLength() { return arrayLength; }
            @Override public long depth() { return 1; }
            @Override public long references() { return 1; }
            @Override public long streamBytes() { return 10; }
        });
    }

    @Test
    void allowsTypesUsedByTheApplication() {
        for (Class<?> type : new Class<?>[] {String.class, Double.class, Integer.class, ArrayList.class,
                HashMap.class, TreeSet.class, LocalDate.class, java.time.LocalDateTime.class,
                SinhVienDTO.class, TrangThaiHocTap.class, com.sms.common.BusinessException.class,
                RemoteObjectInvocationHandler.class, java.lang.reflect.Proxy.class,
                java.util.List.of().getClass(), String[].class}) {
            assertEquals(Status.ALLOWED, check(type), type.getName());
        }
    }

    private static Class<?> proxyOf(Class<?>... interfaces) {
        return java.lang.reflect.Proxy.newProxyInstance(SerialFilterTest.class.getClassLoader(), interfaces,
                (proxy, method, args) -> null).getClass();
    }

    @Test
    void allowsPhotoSizedByteArraysButNotHugeOnes() {
        assertNotEquals(Status.REJECTED, check(byte[].class, 1_048_576)); // ảnh hồ sơ tối đa 1 MiB (UNDECIDED = được nhận)
        assertEquals(Status.REJECTED, check(byte[].class, 3_000_000));  // vượt giới hạn mảng
    }

    @Test
    void allowsOnlyRemoteStubProxies() {
        assertEquals(Status.ALLOWED, check(proxyOf(com.sms.common.rmi.AuthService.class)));
        assertEquals(Status.ALLOWED, check(proxyOf(java.rmi.registry.Registry.class)));
        assertEquals(Status.REJECTED, check(proxyOf(Runnable.class)));
        assertEquals(Status.REJECTED, check(proxyOf(java.rmi.Remote.class, Runnable.class)));
    }

    @Test
    void rejectsEverythingElse() {
        for (Class<?> type : new Class<?>[] {java.io.File.class, java.net.URL.class,
                java.util.concurrent.ConcurrentHashMap.class, java.util.logging.Logger.class,
                javax.naming.InitialContext.class,
                org.slf4j.LoggerFactory.class}) {
            assertEquals(Status.REJECTED, check(type), type.getName());
        }
    }
}
