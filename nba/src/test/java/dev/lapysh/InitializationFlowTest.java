package dev.lapysh;

import dev.lapysh.stats.model.PlayerStatistics;
import dev.lapysh.stats.model.TeamStatistics;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for the initialization flow of the NBA application.
 * This test verifies that data can be properly inserted into the database,
 * and that the endpoints return the expected data.
 * Additionally, it verifies the logic of MapStores and the synchronization
 * of Hazelcast with the database on application startup.
 */
@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitializationFlowTest implements TestPropertyProvider {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2-alpine")
        .withDatabaseName("postgres");

    @Inject
    RequestSpecification spec;

    @Inject
    DSLContext dslContext;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @NonNull Map<String, String> getProperties() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        return Map.of(
            "datasources.default.url", postgres.getJdbcUrl(),
            "datasources.default.username", postgres.getUsername(),
            "datasources.default.password", postgres.getPassword(),
            "r2dbc.datasources.r2dbc.url", postgres.getJdbcUrl(),
            "r2dbc.datasources.r2dbc.port", String.valueOf(postgres.getFirstMappedPort()),
            "r2dbc.datasources.r2dbc.username", postgres.getUsername(),
            "r2dbc.datasources.r2dbc.password", postgres.getPassword()
        );
    }

    @BeforeAll
    void setUp() {
        // Extract values for players_data
        List<Map<String, Object>> playersData = List.of(
            Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("player_name", "Player1"),
                Map.entry("team_name", "Team1"),
                Map.entry("season_name", "2023-2024"),
                Map.entry("game_id", UUID.randomUUID()),
                Map.entry("points", 25),
                Map.entry("rebounds", 10),
                Map.entry("assists", 5),
                Map.entry("steals", 3),
                Map.entry("blocks", 2),
                Map.entry("fouls", 1),
                Map.entry("turnovers", 4),
                Map.entry("minutes_played", 30.5),
                Map.entry("created_at", new Timestamp(System.currentTimeMillis()))
            ),
            Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("player_name", "Player2"),
                Map.entry("team_name", "Team1"),
                Map.entry("season_name", "2023-2024"),
                Map.entry("game_id", UUID.randomUUID()),
                Map.entry("points", 30),
                Map.entry("rebounds", 8),
                Map.entry("assists", 6),
                Map.entry("steals", 2),
                Map.entry("blocks", 3),
                Map.entry("fouls", 2),
                Map.entry("turnovers", 3),
                Map.entry("minutes_played", 35.0),
                Map.entry("created_at", new Timestamp(System.currentTimeMillis()))
            ),
            Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("player_name", "Player3"),
                Map.entry("team_name", "Team2"),
                Map.entry("season_name", "2023-2024"),
                Map.entry("game_id", UUID.randomUUID()),
                Map.entry("points", 20),
                Map.entry("rebounds", 9),
                Map.entry("assists", 7),
                Map.entry("steals", 1),
                Map.entry("blocks", 1),
                Map.entry("fouls", 0),
                Map.entry("turnovers", 2),
                Map.entry("minutes_played", 28.0),
                Map.entry("created_at", new Timestamp(System.currentTimeMillis()))
            ),
            Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("player_name", "Player1"),
                Map.entry("team_name", "Team2"),
                Map.entry("season_name", "2023-2024"),
                Map.entry("game_id", UUID.randomUUID()),
                Map.entry("points", 15),
                Map.entry("rebounds", 6),
                Map.entry("assists", 4),
                Map.entry("steals", 1),
                Map.entry("blocks", 1),
                Map.entry("fouls", 2),
                Map.entry("turnovers", 3),
                Map.entry("minutes_played", 32.0),
                Map.entry("created_at", new Timestamp(System.currentTimeMillis()))
            ),
            Map.ofEntries(
                Map.entry("id", UUID.randomUUID()),
                Map.entry("player_name", "Player3"),
                Map.entry("team_name", "Team1"),
                Map.entry("season_name", "2023-2024"),
                Map.entry("game_id", UUID.randomUUID()),
                Map.entry("points", 22),
                Map.entry("rebounds", 11),
                Map.entry("assists", 8),
                Map.entry("steals", 4),
                Map.entry("blocks", 2),
                Map.entry("fouls", 3),
                Map.entry("turnovers", 1),
                Map.entry("minutes_played", 34.0),
                Map.entry("created_at", new Timestamp(System.currentTimeMillis()))
            )
        );

        // Insert data into players_data
        Flux.from(dslContext.batch(
            playersData.stream().map(data ->
                dslContext.insertInto(table("players_data"),
                        field("id"), field("player_name"), field("team_name"), field("season_name"), field("game_id"),
                        field("points"), field("rebounds"), field("assists"), field("steals"), field("blocks"), field("fouls"),
                        field("turnovers"), field("minutes_played"), field("created_at"))
                    .values(
                        data.get("id"), data.get("player_name"), data.get("team_name"), data.get("season_name"), data.get("game_id"),
                        data.get("points"), data.get("rebounds"), data.get("assists"), data.get("steals"), data.get("blocks"),
                        data.get("fouls"), data.get("turnovers"), data.get("minutes_played"), data.get("created_at"))
            ).collect(Collectors.toList())
        )).blockLast();

        // Calculate statistics for players
        Map<String, Map<String, Double>> playerStats = playersData.stream()
            .collect(Collectors.groupingBy(data -> data.get("player_name").toString()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Map<String, Object>> stats = entry.getValue();
                    int count = stats.size();
                    double avgPoints = stats.stream().mapToDouble(data -> (int) data.get("points")).average().orElse(0.0);
                    double avgRebounds = stats.stream().mapToDouble(data -> (int) data.get("rebounds")).average().orElse(0.0);
                    double avgAssists = stats.stream().mapToDouble(data -> (int) data.get("assists")).average().orElse(0.0);
                    double avgSteals = stats.stream().mapToDouble(data -> (int) data.get("steals")).average().orElse(0.0);
                    double avgBlocks = stats.stream().mapToDouble(data -> (int) data.get("blocks")).average().orElse(0.0);
                    double avgFouls = stats.stream().mapToDouble(data -> (int) data.get("fouls")).average().orElse(0.0);
                    double avgTurnovers = stats.stream().mapToDouble(data -> (int) data.get("turnovers")).average().orElse(0.0);
                    double avgMinutesPlayed = stats.stream().mapToDouble(data -> (double) data.get("minutes_played")).average().orElse(0.0);
                    return Map.ofEntries(
                        Map.entry("avg_points", avgPoints), Map.entry("avg_rebounds", avgRebounds), Map.entry("avg_assists", avgAssists),
                        Map.entry("avg_steals", avgSteals), Map.entry("avg_blocks", avgBlocks), Map.entry("avg_fouls", avgFouls),
                        Map.entry("avg_turnovers", avgTurnovers), Map.entry("avg_minutes_played", avgMinutesPlayed), Map.entry("count_of_rows", (double) count)
                    );
                }
            ));

        // Insert data into statistics_players
        Flux.from(dslContext.batch(
            playerStats.entrySet().stream().map(entry ->
                dslContext.insertInto(table("statistics_players"),
                        field("player_name"), field("season_name"), field("avg_points"), field("avg_rebounds"), field("avg_assists"),
                        field("avg_steals"), field("avg_blocks"), field("avg_fouls"), field("avg_turnovers"), field("avg_minutes_played"),
                        field("count_of_rows"), field("updated_at"))
                    .values(
                        entry.getKey(), "2023-2024", entry.getValue().get("avg_points"), entry.getValue().get("avg_rebounds"),
                        entry.getValue().get("avg_assists"), entry.getValue().get("avg_steals"), entry.getValue().get("avg_blocks"),
                        entry.getValue().get("avg_fouls"), entry.getValue().get("avg_turnovers"), entry.getValue().get("avg_minutes_played"),
                        entry.getValue().get("count_of_rows").intValue(), new Timestamp(System.currentTimeMillis()))
            ).collect(Collectors.toList())
        )).blockLast();

        // Calculate statistics for teams
        Map<String, Map<String, Double>> teamStats = playersData.stream()
            .collect(Collectors.groupingBy(data -> data.get("team_name").toString()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Map<String, Object>> stats = entry.getValue();
                    int count = stats.size();
                    double avgPoints = stats.stream().mapToDouble(data -> (int) data.get("points")).average().orElse(0.0);
                    double avgRebounds = stats.stream().mapToDouble(data -> (int) data.get("rebounds")).average().orElse(0.0);
                    double avgAssists = stats.stream().mapToDouble(data -> (int) data.get("assists")).average().orElse(0.0);
                    double avgSteals = stats.stream().mapToDouble(data -> (int) data.get("steals")).average().orElse(0.0);
                    double avgBlocks = stats.stream().mapToDouble(data -> (int) data.get("blocks")).average().orElse(0.0);
                    double avgFouls = stats.stream().mapToDouble(data -> (int) data.get("fouls")).average().orElse(0.0);
                    double avgTurnovers = stats.stream().mapToDouble(data -> (int) data.get("turnovers")).average().orElse(0.0);
                    double avgMinutesPlayed = stats.stream().mapToDouble(data -> (double) data.get("minutes_played")).average().orElse(0.0);
                    return Map.ofEntries(
                        Map.entry("avg_points", avgPoints), Map.entry("avg_rebounds", avgRebounds), Map.entry("avg_assists", avgAssists),
                        Map.entry("avg_steals", avgSteals), Map.entry("avg_blocks", avgBlocks), Map.entry("avg_fouls", avgFouls),
                        Map.entry("avg_turnovers", avgTurnovers), Map.entry("avg_minutes_played", avgMinutesPlayed), Map.entry("count_of_rows", (double) count)
                    );
                }
            ));

        // Insert data into statistics_teams
        Flux.from(dslContext.batch(
            teamStats.entrySet().stream().map(entry ->
                dslContext.insertInto(table("statistics_teams"),
                        field("team_name"), field("season_name"), field("avg_points"), field("avg_rebounds"), field("avg_assists"),
                        field("avg_steals"), field("avg_blocks"), field("avg_fouls"), field("avg_turnovers"), field("avg_minutes_played"),
                        field("count_of_rows"), field("updated_at"))
                    .values(
                        entry.getKey(), "2023-2024", entry.getValue().get("avg_points"), entry.getValue().get("avg_rebounds"),
                        entry.getValue().get("avg_assists"), entry.getValue().get("avg_steals"), entry.getValue().get("avg_blocks"),
                        entry.getValue().get("avg_fouls"), entry.getValue().get("avg_turnovers"), entry.getValue().get("avg_minutes_played"),
                        entry.getValue().get("count_of_rows").intValue(), new Timestamp(System.currentTimeMillis()))
            ).collect(Collectors.toList())
        )).blockLast();
    }

    @Test
    void testInitializationFlow() {
        // Wait 30 seconds to allow endpoints to return data
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // Validate /stats/players endpoint
                var playerResponseLines = IOUtils.readLines(
                    given(spec)
                        .when()
                        .log()
                        .all()
                        .get("/stats/players?season=2023-2024")
                        .then()
                        .log()
                        .all()
                        .statusCode(200)
                        .extract()
                        .asInputStream(),
                    StandardCharsets.UTF_8
                );
                var playerResponse = playerResponseLines.stream()
                    .map(line -> {
                        try {
                            return objectMapper.readValue(line, PlayerStatistics.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse JSON line: " + line, e);
                        }
                    })
                    .collect(Collectors.toList());

                var expectedPlayerStats = Flux.from(dslContext.select(
                            field("player_name"),
                            field("avg_points"),
                            field("avg_rebounds"),
                            field("avg_assists"),
                            field("avg_steals"),
                            field("avg_blocks"),
                            field("avg_fouls"),
                            field("avg_turnovers"),
                            field("avg_minutes_played"))
                        .from(table("statistics_players"))
                        .where(field("season_name").eq("2023-2024")))
                    .map(record -> new PlayerStatistics(
                        record.get(field("player_name"), String.class),
                        record.get(field("avg_points"), Double.class),
                        record.get(field("avg_rebounds"), Double.class),
                        record.get(field("avg_assists"), Double.class),
                        record.get(field("avg_steals"), Double.class),
                        record.get(field("avg_blocks"), Double.class),
                        record.get(field("avg_fouls"), Double.class),
                        record.get(field("avg_turnovers"), Double.class),
                        record.get(field("avg_minutes_played"), Double.class)
                    ))
                    .collect(Collectors.toList())
                    .block();

                assertNotNull(playerResponse);
                assertEquals(expectedPlayerStats.size(), playerResponse.size());

                playerResponse.forEach(stat -> {
                    var expected = expectedPlayerStats.stream()
                        .filter(s -> s.playerName().equals(stat.playerName()))
                        .findFirst()
                        .orElse(null);
                    assertNotNull(expected);
                    assertEquals(expected.avgPoints(), stat.avgPoints(), 0.001);
                    assertEquals(expected.avgRebounds(), stat.avgRebounds(), 0.001);
                    assertEquals(expected.avgAssists(), stat.avgAssists(), 0.001);
                    assertEquals(expected.avgSteals(), stat.avgSteals(), 0.001);
                    assertEquals(expected.avgBlocks(), stat.avgBlocks(), 0.001);
                    assertEquals(expected.avgFouls(), stat.avgFouls(), 0.001);
                    assertEquals(expected.avgTurnovers(), stat.avgTurnovers(), 0.001);
                    assertEquals(expected.avgMinutesPlayed(), stat.avgMinutesPlayed(), 0.001);
                });

                // Validate /stats/teams endpoint
                var teamResponseLines = IOUtils.readLines(
                    given(spec)
                        .when()
                        .log()
                        .all()
                        .get("/stats/teams?season=2023-2024")
                        .then()
                        .log()
                        .all()
                        .statusCode(200)
                        .extract()
                        .asInputStream(),
                    StandardCharsets.UTF_8
                );
                var teamResponse = teamResponseLines.stream()
                    .map(line -> {
                        try {
                            return objectMapper.readValue(line, TeamStatistics.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse JSON line: " + line, e);
                        }
                    })
                    .collect(Collectors.toList());

                var expectedTeamStats = Flux.from(dslContext.select(
                            field("team_name"),
                            field("avg_points"),
                            field("avg_rebounds"),
                            field("avg_assists"),
                            field("avg_steals"),
                            field("avg_blocks"),
                            field("avg_fouls"),
                            field("avg_turnovers"),
                            field("avg_minutes_played"))
                        .from(table("statistics_teams"))
                        .where(field("season_name").eq("2023-2024")))
                    .map(record -> new TeamStatistics(
                        record.get(field("team_name"), String.class),
                        record.get(field("avg_points"), Double.class),
                        record.get(field("avg_rebounds"), Double.class),
                        record.get(field("avg_assists"), Double.class),
                        record.get(field("avg_steals"), Double.class),
                        record.get(field("avg_blocks"), Double.class),
                        record.get(field("avg_fouls"), Double.class),
                        record.get(field("avg_turnovers"), Double.class),
                        record.get(field("avg_minutes_played"), Double.class)
                    ))
                    .collect(Collectors.toList())
                    .block();

                assertNotNull(teamResponse);
                assertEquals(expectedTeamStats.size(), teamResponse.size());

                teamResponse.forEach(stat -> {
                    var expected = expectedTeamStats.stream()
                        .filter(s -> s.teamName().equals(stat.teamName()))
                        .findFirst()
                        .orElse(null);
                    assertNotNull(expected);
                    assertEquals(expected.avgPoints(), stat.avgPoints(), 0.001);
                    assertEquals(expected.avgRebounds(), stat.avgRebounds(), 0.001);
                    assertEquals(expected.avgAssists(), stat.avgAssists(), 0.001);
                    assertEquals(expected.avgSteals(), stat.avgSteals(), 0.001);
                    assertEquals(expected.avgBlocks(), stat.avgBlocks(), 0.001);
                    assertEquals(expected.avgFouls(), stat.avgFouls(), 0.001);
                    assertEquals(expected.avgTurnovers(), stat.avgTurnovers(), 0.001);
                    assertEquals(expected.avgMinutesPlayed(), stat.avgMinutesPlayed(), 0.001);
                });
            });
    }
}
