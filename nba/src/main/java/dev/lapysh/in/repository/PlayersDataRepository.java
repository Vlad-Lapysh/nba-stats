package dev.lapysh.in.repository;

import dev.lapysh.in.model.PlayerGameData;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Singleton
public class PlayersDataRepository {

    private final DSLContext dsl;

    public PlayersDataRepository(@Named("r2dbcPooledDslContext") DSLContext dsl) {
        this.dsl = dsl;
    }

    public Mono<Void> save(PlayerGameData playerGameData) {
        return Mono.from(dsl.insertInto(table("players_data"))
                .set(field("id"), DSL.cast(UUID.randomUUID(), UUID.class))
                .set(field("player_name"), playerGameData.getPlayerName())
                .set(field("team_name"), playerGameData.getTeamName())
                .set(field("season_name"), playerGameData.getSeasonName())
                .set(field("game_id"), DSL.cast(playerGameData.getGameId(), UUID.class))
                .set(field("points"), playerGameData.getPoints())
                .set(field("rebounds"), playerGameData.getRebounds())
                .set(field("assists"), playerGameData.getAssists())
                .set(field("steals"), playerGameData.getSteals())
                .set(field("blocks"), playerGameData.getBlocks())
                .set(field("fouls"), playerGameData.getFouls())
                .set(field("turnovers"), playerGameData.getTurnovers())
                .set(field("minutes_played"), playerGameData.getMinutesPlayed())
                .set(field("created_at"), LocalDateTime.now()))
            .then();
    }

}
