package com.sms.server.jpa;

import jakarta.persistence.Persistence;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MappingTest {

    @Test
    void persistenceUnitBootstrapsAllEntitiesWithoutDatabase() {
        try (var factory = Persistence.createEntityManagerFactory("smsPU", Map.of(
                "hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl",
                "hibernate.boot.allow_jdbc_metadata_access", "false",
                "hibernate.hbm2ddl.auto", "none"))) {
            assertEquals(13, factory.getMetamodel().getEntities().size());
        }
    }
}
