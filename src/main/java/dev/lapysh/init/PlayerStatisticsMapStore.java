package dev.lapysh.init;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;
import dev.lapysh.core.model.ContinuousAverage;
import dev.lapysh.core.model.Statistics;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class PlayerStatisticsMapStore implements MapStore<String, Map<String, Statistics>>, MapLoaderLifecycleSupport {

    private static volatile DSLContext dsl;

    public static void setDsl(DSLContext dsl) {
        PlayerStatisticsMapStore.dsl = dsl;
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
    }

    @Override
    public Map<String, Statistics> load(String seasonName) {
        return Flux.from(dsl.select()
                .from(table("statistics_players"))
                .where(field("season_name").eq(seasonName)))
            .map(record -> {
                var countOfRows = record.get("count_of_rows", Integer.class);
                return Map.entry(
                    record.get("player_name", String.class),
                    new Statistics(
                        new ContinuousAverage(countOfRows, record.get("avg_points", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_rebounds", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_assists", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_steals", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_blocks", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_fouls", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_turnovers", Double.class)),
                        new ContinuousAverage(countOfRows, record.get("avg_minutes_played", Double.class))
                    )
                );
            })
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .block();
    }

    @Override
    public Map<String, Map<String, Statistics>> loadAll(Collection<String> keys) {
        return Map.of();
    }

    @Override
    public void store(String seasonName, Map<String, Statistics> playerStats) {
        Flux.fromIterable(playerStats.entrySet())
            .flatMap(entry -> {
                String playerName = entry.getKey();
                Statistics stats = entry.getValue();
                return Mono.from(dsl.insertInto(table("statistics_players"))
                    .columns(
                        field("player_name"),
                        field("season_name"),
                        field("avg_points"),
                        field("avg_rebounds"),
                        field("avg_assists"),
                        field("avg_steals"),
                        field("avg_blocks"),
                        field("avg_fouls"),
                        field("avg_turnovers"),
                        field("avg_minutes_played"),
                        field("count_of_rows"),
                        field("updated_at")
                    )
                    .values(
                        playerName,
                        seasonName,
                        stats.avgPoints().average,
                        stats.avgRebounds().average,
                        stats.avgAssists().average,
                        stats.avgSteals().average,
                        stats.avgBlocks().average,
                        stats.avgFouls().average,
                        stats.avgTurnovers().average,
                        stats.avgMinutesPlayed().average,
                        stats.avgPoints().n,
                        LocalDateTime.now()
                    )
                    .onConflictOnConstraint(DSL.constraint("pk_statistics_players"))
                    .doUpdate()
                    .set(field("avg_points"), stats.avgPoints().average)
                    .set(field("avg_rebounds"), stats.avgRebounds().average)
                    .set(field("avg_assists"), stats.avgAssists().average)
                    .set(field("avg_steals"), stats.avgSteals().average)
                    .set(field("avg_blocks"), stats.avgBlocks().average)
                    .set(field("avg_fouls"), stats.avgFouls().average)
                    .set(field("avg_turnovers"), stats.avgTurnovers().average)
                    .set(field("avg_minutes_played"), stats.avgMinutesPlayed().average)
                    .set(field("count_of_rows"), stats.avgPoints().n)
                    .set(field("updated_at"), LocalDateTime.now())
                );
            })
            .then()
            .block();
    }

    @Override
    public void storeAll(Map<String, Map<String, Statistics>> map) {
        map.forEach(this::store);
    }

    @Override
    public void delete(String key) {
    }

    @Override
    public void deleteAll(Collection<String> keys) {
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return Flux.from(dsl.selectDistinct(field("season_name"))
                .from(table("statistics_players")))
            .map(record -> record.get(field("season_name", String.class)))
            .toIterable();
    }

    @Override
    public void destroy() {
    }

}
