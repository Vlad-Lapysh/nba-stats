package dev.lapysh.cfg;


import dev.lapysh.init.PlayerStatisticsMapStore;
import dev.lapysh.init.TeamStatisticsMapStore;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

@Factory
public class DSLContextFactory {

    @Singleton
    @Named("r2dbcPooledConnectionFactory")
    public ConnectionFactory connectionFactory(@Value("${r2dbc.datasources.r2dbc.username}") String username,
                                               @Value("${r2dbc.datasources.r2dbc.password}") String password,
                                               @Value("${r2dbc.datasources.r2dbc.host}") String host,
                                               @Value("${r2dbc.datasources.r2dbc.port}") String port,
                                               @Value("${r2dbc.datasources.r2dbc.options.max-size}") int maxSize,
                                               @Value("${r2dbc.datasources.r2dbc.options.initial-size}") int initialSize) {

        var connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql")
            .option(HOST, host)
            .option(PORT, Integer.valueOf(port))
            .option(USER, username)
            .option(PASSWORD, password)
            .option(DATABASE, "postgres")
            .build());
        var configuration = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxIdleTime(Duration.ofMinutes(30))
            .maxSize(maxSize)
            .initialSize(initialSize)
            .build();
        return new ConnectionPool(configuration);
    }

    @Singleton
    @Named("r2dbcPooledDslContext")
    public DSLContext dslContext(@Named("r2dbcPooledConnectionFactory") ConnectionFactory connectionFactory) {
        var dslContext = DSL.using(connectionFactory);
        // Hack to provide dependency to unmanaged bean
        PlayerStatisticsMapStore.setDsl(dslContext);
        TeamStatisticsMapStore.setDsl(dslContext);
        return dslContext;
    }
}
