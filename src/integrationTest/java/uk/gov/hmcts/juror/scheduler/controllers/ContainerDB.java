package uk.gov.hmcts.juror.scheduler.controllers;

import org.flywaydb.core.Flyway;
import org.junit.ClassRule;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
@Order(Integer.MIN_VALUE)
@ActiveProfiles({"test"})
class ContainerDB {
    private static final String DOCKER_IMAGE = "postgres:15-alpine";

    @ClassRule
    protected static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER;

    static {
        POSTGRE_SQL_CONTAINER
            = new PostgreSQLContainer<>(DOCKER_IMAGE)
            .withDatabaseName("scheduler_application")
            .withUsername("postgres")
            .withPassword("postgres")
            .withExposedPorts(5432);
        POSTGRE_SQL_CONTAINER.setPortBindings(List.of(
            "5433:5432"
        ));
        POSTGRE_SQL_CONTAINER.start();
        String jdbcUrl = POSTGRE_SQL_CONTAINER.getJdbcUrl();
        String username = POSTGRE_SQL_CONTAINER.getUsername();
        String password = POSTGRE_SQL_CONTAINER.getPassword();
        Flyway flyway = Flyway.configure()
            .defaultSchema("scheduler_application")
            .table("schema_history")
            .dataSource(jdbcUrl, username, password)
            .locations("filesystem:src/main/resources/db/migrations")
            .load();
        flyway.migrate();
    }

    @Test
    void testDatabaseExists() throws SQLException {
        String jdbcUrl = POSTGRE_SQL_CONTAINER.getJdbcUrl();
        String username = POSTGRE_SQL_CONTAINER.getUsername();
        String password = POSTGRE_SQL_CONTAINER.getPassword();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             ResultSet resultSet =
                 conn.createStatement().executeQuery("SELECT count(*) FROM scheduler_application.schema_history")) {
            if (resultSet.next()) {    // result is properly examined and used
                assertNotEquals(0, resultSet.getInt(1),
                    "Does schema_history table exist");
            }
        }
    }
}
