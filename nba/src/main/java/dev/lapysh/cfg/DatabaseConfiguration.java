package dev.lapysh.cfg;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.jdbc.BasicJdbcConfiguration;

@ConfigurationProperties(BasicJdbcConfiguration.PREFIX + ".default")
public record DatabaseConfiguration(
    String url,
    String username,
    String password) {
}
