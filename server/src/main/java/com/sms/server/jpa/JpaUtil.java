package com.sms.server.jpa;

import com.sms.server.config.AppConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.util.Objects;
import java.util.function.Function;

public final class JpaUtil implements AutoCloseable {

    private final EntityManagerFactory factory;

    public JpaUtil(AppConfig config) {
        this(Persistence.createEntityManagerFactory("smsPU", config.jdbcProperties()));
    }

    JpaUtil(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public <T> T inTransaction(Function<EntityManager, T> work) {
        try (EntityManager em = factory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                T result = work.apply(em);
                tx.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                try {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                } catch (RuntimeException | Error rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    public <T> T query(Function<EntityManager, T> work) {
        // Cả truy vấn và mapper chạy trong transaction để đọc nhất quán và tải được LAZY.
        return inTransaction(work);
    }

    @Override
    public synchronized void close() {
        if (factory.isOpen()) {
            factory.close();
        }
    }
}
