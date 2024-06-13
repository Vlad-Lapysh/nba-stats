package dev.lapysh.cfg;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.liquibase.LiquibaseConfigurationProperties;
import io.micronaut.liquibase.LiquibaseMigrator;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LiquibaseMigrationListener implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseMigrationListener.class);

    private final LiquibaseMigrator liquibaseMigrator;
    private final DatabaseConfiguration configuration;
    private final LiquibaseConfigurationProperties liquibaseConfigurationProperties;
    private final ApplicationEventPublisher<LiquibaseMigrationCompletedEvent> eventPublisher;

    public LiquibaseMigrationListener(LiquibaseMigrator liquibaseMigrator,
                                      DatabaseConfiguration configuration,
                                      LiquibaseConfigurationProperties liquibaseConfigurationProperties,
                                      ApplicationEventPublisher<LiquibaseMigrationCompletedEvent> eventPublisher) {
        this.liquibaseMigrator = liquibaseMigrator;
        this.configuration = configuration;
        this.liquibaseConfigurationProperties = liquibaseConfigurationProperties;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        LOG.info("Running Liquibase migrations...");
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(configuration.url());
        dataSource.setUser(configuration.username());
        dataSource.setPassword(configuration.password());
        try {
            liquibaseMigrator.run(liquibaseConfigurationProperties, dataSource);
            eventPublisher.publishEvent(new LiquibaseMigrationCompletedEvent());
            LOG.info("Liquibase migrations completed successfully.");
        } catch (Exception e) {
            LOG.error("Failed to run Liquibase migrations", e);
            throw new RuntimeException("Failed to run Liquibase migrations", e);
        }
    }

    public record LiquibaseMigrationCompletedEvent() {
    }
}
