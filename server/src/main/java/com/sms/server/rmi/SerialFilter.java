package com.sms.server.rmi;

import java.io.ObjectInputFilter;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Bộ lọc deserialization cho mọi lời gọi RMI (chống gadget chain): chỉ cho phép các gói dự án,
 * kiểu cơ bản của JDK và hạ tầng RMI; từ chối tất cả còn lại.
 */
@Slf4j
public final class SerialFilter {

    /** Mẫu bộ lọc theo plan mục 6, thêm giới hạn độ sâu và kích thước mảng. */
    static final String PATTERN = "maxdepth=50;maxarray=2097152;"
            + "com.sms.common.**;java.lang.*;java.util.*;java.time.*;java.math.*;"
            + "java.rmi.**;java.lang.reflect.Proxy;!*";

    private SerialFilter() {
    }

    static ObjectInputFilter create() {
        ObjectInputFilter pattern = ObjectInputFilter.Config.createFilter(PATTERN);
        return info -> {
            Class<?> type = info.serialClass();
            // Stub RMI là lớp proxy động (jdk.proxyN.$ProxyM) nên mẫu theo tên không khớp được;
            // chỉ cho phép proxy mà mọi interface đều là interface Remote của dự án hoặc java.rmi.
            if (type != null && Proxy.isProxyClass(type)) {
                boolean remoteOnly = Arrays.stream(type.getInterfaces()).allMatch(i ->
                        Remote.class.isAssignableFrom(i)
                                && (i.getName().startsWith("com.sms.common.") || i.getName().startsWith("java.rmi.")));
                return remoteOnly ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            }
            return pattern.checkInput(info);
        };
    }

    /** Cài bộ lọc cho cả tiến trình; chỉ làm được một lần nên gọi lặp lại là vô hại. */
    public static synchronized void install() {
        if (ObjectInputFilter.Config.getSerialFilter() != null) {
            log.info("Bộ lọc serialization đã được cài trước đó, giữ nguyên.");
            return;
        }
        ObjectInputFilter.Config.setSerialFilter(create());
        log.info("Đã bật bộ lọc serialization: {}", PATTERN);
    }
}
