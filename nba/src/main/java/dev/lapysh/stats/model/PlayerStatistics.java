package dev.lapysh.stats.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerStatistics(
    String playerName,
    double avgPoints,
    double avgRebounds,
    double avgAssists,
    double avgSteals,
    double avgBlocks,
    double avgFouls,
    double avgTurnovers,
    double avgMinutesPlayed
) implements StatisticsResponse {
    @JsonCreator
    public PlayerStatistics(@JsonProperty("playerName") String playerName,
                            @JsonProperty("avgPoints") double avgPoints,
                            @JsonProperty("avgRebounds") double avgRebounds,
                            @JsonProperty("avgAssists") double avgAssists,
                            @JsonProperty("avgSteals") double avgSteals,
                            @JsonProperty("avgBlocks") double avgBlocks,
                            @JsonProperty("avgFouls") double avgFouls,
                            @JsonProperty("avgTurnovers") double avgTurnovers,
                            @JsonProperty("avgMinutesPlayed") double avgMinutesPlayed) {
        this.playerName = playerName;
        this.avgPoints = avgPoints;
        this.avgRebounds = avgRebounds;
        this.avgAssists = avgAssists;
        this.avgSteals = avgSteals;
        this.avgBlocks = avgBlocks;
        this.avgFouls = avgFouls;
        this.avgTurnovers = avgTurnovers;
        this.avgMinutesPlayed = avgMinutesPlayed;
    }
}
