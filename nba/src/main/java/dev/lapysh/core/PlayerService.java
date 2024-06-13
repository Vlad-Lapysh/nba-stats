package dev.lapysh.core;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dev.lapysh.core.model.Statistics;
import dev.lapysh.in.model.PlayerGameData;
import dev.lapysh.in.repository.PlayersDataRepository;
import dev.lapysh.stats.model.PlayerStatistics;
import dev.lapysh.stats.model.TeamStatistics;
import io.micronaut.context.annotation.Context;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Context
@Singleton
public class PlayerService {

    private final PlayersDataRepository repository;
    private final IMap<String, Map<String, Statistics>> playerDataMap;
    private final IMap<String, Map<String, Statistics>> teamDataMap;

    public PlayerService(PlayersDataRepository repository,
                         HazelcastInstance hcInst) {
        this.repository = repository;
        this.playerDataMap = hcInst.getMap("playerDataMap");
        this.teamDataMap = hcInst.getMap("teamDataMap");
    }

    private Mono<PlayerGameData> validatePlayerGameData(PlayerGameData playerGameData) {
        var validationStatus = Optional.of(playerGameData)
            .map(data -> {
                if (data.getPlayerName() == null || data.getPlayerName().isEmpty())
                    return "Player name must not be empty";
                if (data.getTeamName() == null || data.getTeamName().isEmpty())
                    return "Team name must not be empty";
                if (data.getSeasonName() == null || data.getSeasonName().isEmpty())
                    return "Season name must not be empty";
                if (data.getPoints() < 0) return "Points must be positive";
                if (data.getRebounds() < 0) return "Rebounds must be positive";
                if (data.getAssists() < 0) return "Assists must be positive";
                if (data.getSteals() < 0) return "Steals must be positive";
                if (data.getBlocks() < 0) return "Blocks must be positive";
                if (data.getTurnovers() < 0) return "Turnovers must be positive";
                if (data.getFouls() < 0 || data.getFouls() > 6) return "Fouls must be between 0 and 6";
                if (data.getMinutesPlayed() < 0.0 || data.getMinutesPlayed() > 48.0)
                    return "Minutes played must be between 0.0 and 48.0";
                return null;
            })
            .orElse("ok");

        return validationStatus.equals("ok") ?
            Mono.just(playerGameData) :
            Mono.error(new IllegalArgumentException(validationStatus));
    }

    public Mono<Void> savePlayerData(PlayerGameData playerGameData) {
        return validatePlayerGameData(playerGameData)
            .flatMap(validData -> Mono.when(
                    Mono.fromCompletionStage(() ->
                        playerDataMap.submitToKey(
                            validData.getSeasonName(),
                            new MapEntryProcessor(validData.getPlayerName(), playerGameData)
                        )
                    ),
                    Mono.fromCompletionStage(() ->
                        teamDataMap.submitToKey(
                            validData.getSeasonName(),
                            new MapEntryProcessor(validData.getTeamName(), playerGameData)
                        )
                    ),
                    repository.save(validData)
                )
            );
    }

    public Mono<List<PlayerStatistics>> calculateAndStorePlayerStatistics(String seasonName) {
        return Mono.fromCompletionStage(() -> playerDataMap.getAsync(seasonName))
            .flatMapMany(seasonStats -> Flux.fromIterable(seasonStats.entrySet()))
            .map(entry -> new PlayerStatistics(
                entry.getKey(),
                entry.getValue().avgPoints().average,
                entry.getValue().avgRebounds().average,
                entry.getValue().avgAssists().average,
                entry.getValue().avgSteals().average,
                entry.getValue().avgBlocks().average,
                entry.getValue().avgFouls().average,
                entry.getValue().avgTurnovers().average,
                entry.getValue().avgMinutesPlayed().average
            ))
            .collectList();
    }

    public Mono<List<TeamStatistics>> calculateAndStoreTeamStatistics(String seasonName) {
        return Mono.fromCompletionStage(() -> teamDataMap.getAsync(seasonName))
            .flatMapMany(seasonStats -> Flux.fromIterable(seasonStats.entrySet()))
            .map(entry -> new TeamStatistics(
                entry.getKey(),
                entry.getValue().avgPoints().average,
                entry.getValue().avgRebounds().average,
                entry.getValue().avgAssists().average,
                entry.getValue().avgSteals().average,
                entry.getValue().avgBlocks().average,
                entry.getValue().avgFouls().average,
                entry.getValue().avgTurnovers().average,
                entry.getValue().avgMinutesPlayed().average
            ))
            .collectList();
    }

}
