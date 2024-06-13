package dev.lapysh;

import com.hazelcast.core.HazelcastInstance;
import dev.lapysh.in.model.PlayerGameData;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NbaTest implements TestPropertyProvider {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2-alpine")
        .withDatabaseName("postgres");

    @Inject
    RequestSpecification spec;

    @Inject
    DSLContext dslContext;

    @Inject
    HazelcastInstance hazelcastInstance;

    ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

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

    @AfterEach
    void tearDown() {
        Flux.from(dslContext.query("TRUNCATE players_data CASCADE"))
            .thenMany(Flux.from(dslContext.query("TRUNCATE statistics_players CASCADE")))
            .thenMany(Mono.fromRunnable(() -> hazelcastInstance.getMap("playerDataMap").clear()))
            .thenMany(Flux.from(dslContext.query("TRUNCATE statistics_teams CASCADE")))
            .thenMany(Mono.fromRunnable(() -> hazelcastInstance.getMap("teamDataMap").clear()))
            .blockLast();
    }

    @Test
    void testSaveAndRetrievePlayerStats() {
        var seasons = List.of("2023-2024", "2022-2023", "2021-2022");
        var playersData = generateRandomPlayerDataForSeasons(seasons, 5);

        // Save player data
        playersData.forEach(playerData ->
            given(spec)
                .contentType("application/json")
                .body(playerData)
                .log()
                .all()
                .when()
                .post("/save")
                .then()
                .log()
                .all()
                .statusCode(200)
        );

        // Retrieve and verify player statistics for each season
        for (var season : seasons) {
            var responseLines = IOUtils.readLines(
                given(spec)
                    .when()
                    .log()
                    .all()
                    .get("/stats/players?season=" + season)
                    .then()
                    .log()
                    .all()
                    .statusCode(200)
                    .extract()
                    .asInputStream(),
                StandardCharsets.UTF_8
            );
            var response = responseLines.stream()
                .map(line -> {
                    try {
                        return objectMapper.readValue(line, PlayerStatistics.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON line: " + line, e);
                    }
                })
                .collect(Collectors.toList());

            var expectedStats = calculateExpectedPlayerStatistics(playersData, season);
            assertNotNull(response);
            assertEquals(expectedStats.size(), response.size());

            response.forEach(stat -> {
                var expected = expectedStats.get(stat.playerName());
                assertEquals(expected.avgPoints(), stat.avgPoints(), 0.001);
                assertEquals(expected.avgRebounds(), stat.avgRebounds(), 0.001);
                assertEquals(expected.avgAssists(), stat.avgAssists(), 0.001);
                assertEquals(expected.avgSteals(), stat.avgSteals(), 0.001);
                assertEquals(expected.avgBlocks(), stat.avgBlocks(), 0.001);
                assertEquals(expected.avgFouls(), stat.avgFouls(), 0.001);
                assertEquals(expected.avgTurnovers(), stat.avgTurnovers(), 0.001);
                assertEquals(expected.avgMinutesPlayed(), stat.avgMinutesPlayed(), 0.001);
            });
        }

        for (var season : seasons) {
            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS) // sync lag, configured in hazelcast.xml write-delay-seconds
                .untilAsserted(() -> {
                    var dbStats = Flux.from(dslContext.select(
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
                            .where(field("season_name").eq(season)))
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
                        .collectList()
                        .block();

                    var expectedStats = calculateExpectedPlayerStatistics(playersData, season);
                    assertNotNull(dbStats);
                    assertEquals(expectedStats.size(), dbStats.size());

                    dbStats.forEach(stat -> {
                        var expected = expectedStats.get(stat.playerName());
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

    @Test
    void testSaveAndRetrieveTeamStats() {
        var seasons = List.of("2023-2024", "2022-2023", "2021-2022");
        var playersData = generateRandomPlayerDataForSeasons(seasons, 5);

        // Save player data
        playersData.forEach(playerData ->
            given(spec)
                .contentType("application/json")
                .body(playerData)
                .log()
                .all()
                .when()
                .post("/save")
                .then()
                .log()
                .all()
                .statusCode(200)
        );

        // Retrieve and verify team statistics for each season
        for (var season : seasons) {
            var responseLines = IOUtils.readLines(
                given(spec)
                    .when()
                    .log()
                    .all()
                    .get("/stats/teams?season=" + season)
                    .then()
                    .log()
                    .all()
                    .statusCode(200)
                    .extract()
                    .asInputStream(),
                StandardCharsets.UTF_8
            );
            var response = responseLines.stream()
                .map(line -> {
                    try {
                        return objectMapper.readValue(line, TeamStatistics.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON line: " + line, e);
                    }
                })
                .collect(Collectors.toList());

            var expectedStats = calculateExpectedTeamStatistics(playersData, season);
            assertNotNull(response);
            assertEquals(expectedStats.size(), response.size());

            response.forEach(stat -> {
                var expected = expectedStats.get(stat.teamName());
                assertEquals(expected.avgPoints(), stat.avgPoints(), 0.001);
                assertEquals(expected.avgRebounds(), stat.avgRebounds(), 0.001);
                assertEquals(expected.avgAssists(), stat.avgAssists(), 0.001);
                assertEquals(expected.avgSteals(), stat.avgSteals(), 0.001);
                assertEquals(expected.avgBlocks(), stat.avgBlocks(), 0.001);
                assertEquals(expected.avgFouls(), stat.avgFouls(), 0.001);
                assertEquals(expected.avgTurnovers(), stat.avgTurnovers(), 0.001);
                assertEquals(expected.avgMinutesPlayed(), stat.avgMinutesPlayed(), 0.001);
            });
        }

        for (var season : seasons) {
            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS) // sync lag, configured in hazelcast.xml write-delay-seconds
                .untilAsserted(() -> {
                    var dbStats = Flux.from(dslContext.select(
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
                            .where(field("season_name").eq(season)))
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
                        .collectList()
                        .block();

                    var expectedStats = calculateExpectedTeamStatistics(playersData, season);
                    assertNotNull(dbStats);
                    assertEquals(expectedStats.size(), dbStats.size());

                    dbStats.forEach(stat -> {
                        var expected = expectedStats.get(stat.teamName());
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

    private List<PlayerGameData> generateRandomPlayerDataForSeasons(List<String> seasons, int countPerSeason) {
        return seasons.stream()
            .flatMap(season -> generateRandomPlayerData(season, countPerSeason).stream())
            .collect(Collectors.toList());
    }

    private List<PlayerGameData> generateRandomPlayerData(String season, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new PlayerGameData(
                UUID.randomUUID(),
                "Player" + i,
                "Team" + (i % 3),
                season,
                UUID.randomUUID(),
                random.nextInt(51), // points: 0-50
                random.nextInt(21), // rebounds: 0-20
                random.nextInt(16), // assists: 0-15
                random.nextInt(11), // steals: 0-10
                random.nextInt(6),  // blocks: 0-5
                random.nextInt(11), // turnovers: 0-10
                random.nextInt(7),  // fouls: 0-6
                random.nextFloat() * 48 // minutes played: 0.0-48.0
            ))
            .collect(Collectors.toList());
    }

    private Map<String, PlayerStatistics> calculateExpectedPlayerStatistics(List<PlayerGameData> playersData,
                                                                            String season) {
        return playersData.stream()
            .filter(data -> data.getSeasonName().equals(season))
            .collect(Collectors.toMap(
                PlayerGameData::getPlayerName,
                data -> new PlayerStatistics(
                    data.getPlayerName(),
                    data.getPoints(),
                    data.getRebounds(),
                    data.getAssists(),
                    data.getSteals(),
                    data.getBlocks(),
                    data.getFouls(),
                    data.getTurnovers(),
                    data.getMinutesPlayed()
                ),
                (stat1, stat2) -> new PlayerStatistics(
                    stat1.playerName(),
                    (stat1.avgPoints() + stat2.avgPoints()) / 2,
                    (stat1.avgRebounds() + stat2.avgRebounds()) / 2,
                    (stat1.avgAssists() + stat2.avgAssists()) / 2,
                    (stat1.avgSteals() + stat2.avgSteals()) / 2,
                    (stat1.avgBlocks() + stat2.avgBlocks()) / 2,
                    (stat1.avgFouls() + stat2.avgFouls()) / 2,
                    (stat1.avgTurnovers() + stat2.avgTurnovers()) / 2,
                    (stat1.avgMinutesPlayed() + stat2.avgMinutesPlayed()) / 2
                )
            ));
    }

    private Map<String, TeamStatistics> calculateExpectedTeamStatistics(List<PlayerGameData> playersData,
                                                                        String season) {
        return playersData.stream()
            .filter(data -> data.getSeasonName().equals(season))
            .collect(Collectors.toMap(
                PlayerGameData::getTeamName,
                data -> new TeamStatistics(
                    data.getTeamName(),
                    data.getPoints(),
                    data.getRebounds(),
                    data.getAssists(),
                    data.getSteals(),
                    data.getBlocks(),
                    data.getFouls(),
                    data.getTurnovers(),
                    data.getMinutesPlayed()
                ),
                (stat1, stat2) -> new TeamStatistics(
                    stat1.teamName(),
                    (stat1.avgPoints() + stat2.avgPoints()) / 2,
                    (stat1.avgRebounds() + stat2.avgRebounds()) / 2,
                    (stat1.avgAssists() + stat2.avgAssists()) / 2,
                    (stat1.avgSteals() + stat2.avgSteals()) / 2,
                    (stat1.avgBlocks() + stat2.avgBlocks()) / 2,
                    (stat1.avgFouls() + stat2.avgFouls()) / 2,
                    (stat1.avgTurnovers() + stat2.avgTurnovers()) / 2,
                    (stat1.avgMinutesPlayed() + stat2.avgMinutesPlayed()) / 2
                )
            ));
    }
}
