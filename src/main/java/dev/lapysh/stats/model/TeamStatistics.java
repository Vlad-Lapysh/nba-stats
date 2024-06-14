// nba/src/main/java/dev/lapysh/stats/model/TeamStatistics.java

package dev.lapysh.stats.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TeamStatistics(
    String teamName,
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
    public TeamStatistics(@JsonProperty("teamName") String teamName,
                          @JsonProperty("avgPoints") double avgPoints,
                          @JsonProperty("avgRebounds") double avgRebounds,
                          @JsonProperty("avgAssists") double avgAssists,
                          @JsonProperty("avgSteals") double avgSteals,
                          @JsonProperty("avgBlocks") double avgBlocks,
                          @JsonProperty("avgFouls") double avgFouls,
                          @JsonProperty("avgTurnovers") double avgTurnovers,
                          @JsonProperty("avgMinutesPlayed") double avgMinutesPlayed) {
        this.teamName = teamName;
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
