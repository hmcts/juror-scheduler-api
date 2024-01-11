package uk.gov.hmcts.juror.scheduler.testsupport;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

public class PostgresqlContainer extends PostgreSQLContainer<PostgresqlContainer> {

    private static final String IMAGE_VERSION = "postgres:16-alpine";
    private static PostgresqlContainer container;

    private PostgresqlContainer() {
        super(IMAGE_VERSION);
        setup();
    }

    public void setup() {
        withDatabaseName("scheduler_application");
        withUsername("system");
        withPassword("postgres");
        withExposedPorts(5432);
        setPortBindings(List.of(
            "5433:5432"
        ));
    }

    public static PostgresqlContainer getInstance() {
        if (container == null) {
            container = new PostgresqlContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());
        String jdbcUrl = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        Flyway flyway = Flyway.configure()
            .defaultSchema("scheduler_application")
            .table("schema_history")
            .dataSource(jdbcUrl, username, password)
            .sqlMigrationPrefix("scheduler-V")
            .load();
        flyway.migrate();
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
