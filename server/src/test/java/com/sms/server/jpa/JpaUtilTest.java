package com.sms.server.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JpaUtilTest {

    private final List<String> calls = new ArrayList<>();
    private boolean active;
    private boolean failCommit;
    private int created;

    private final EntityTransaction tx = proxy(EntityTransaction.class, (method, args) -> {
        switch (method) {
            case "begin" -> { calls.add("begin"); active = true; }
            case "commit" -> {
                calls.add("commit");
                if (failCommit) {
                    throw new IllegalStateException("commit failure");
                }
                active = false;
            }
            case "rollback" -> { calls.add("rollback"); active = false; }
            case "isActive" -> { return active; }
            default -> throw new UnsupportedOperationException(method);
        }
        return null;
    });

    private final EntityManagerFactory factory = proxy(EntityManagerFactory.class, (method, args) -> {
        if (!method.equals("createEntityManager")) {
            throw new UnsupportedOperationException(method);
        }
        created++;
        return proxy(EntityManager.class, (emMethod, emArgs) -> switch (emMethod) {
            case "getTransaction" -> tx;
            case "close" -> { calls.add("close"); yield null; }
            default -> throw new UnsupportedOperationException(emMethod);
        });
    });

    @Test
    void commitsAndClosesWithSeparateManagerPerCall() {
        JpaUtil jpa = new JpaUtil(factory);
        assertEquals("ok", jpa.inTransaction(em -> "ok"));
        assertEquals(12, jpa.query(em -> 12).intValue());
        assertEquals(2, created);
        assertEquals(List.of("begin", "commit", "close", "begin", "commit", "close"), calls);
    }

    @Test
    void rollbackPreservesOriginalExceptionAndCloses() {
        IllegalArgumentException failure = new IllegalArgumentException("failure");
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> new JpaUtil(factory).inTransaction(em -> { throw failure; })));
        assertEquals(List.of("begin", "rollback", "close"), calls);
    }

    @Test
    void commitFailureAlsoRollsBackAndCloses() {
        failCommit = true;
        assertThrows(IllegalStateException.class,
                () -> new JpaUtil(factory).inTransaction(em -> null));
        assertEquals(List.of("begin", "commit", "rollback", "close"), calls);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, args) -> handler.apply(method.getName(), args));
    }
}
