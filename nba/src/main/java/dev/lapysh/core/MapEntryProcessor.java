package dev.lapysh.core;

import com.hazelcast.core.Offloadable;
import com.hazelcast.map.EntryProcessor;
import dev.lapysh.core.model.ContinuousAverage;
import dev.lapysh.core.model.Statistics;
import dev.lapysh.in.model.PlayerGameData;

import java.util.HashMap;
import java.util.Map;

public class MapEntryProcessor implements EntryProcessor<String, Map<String, Statistics>, Void>, Offloadable {
    public String name;
    public PlayerGameData playerGameData;

    public MapEntryProcessor(String name, PlayerGameData playerGameData) {
        this.name = name;
        this.playerGameData = playerGameData;
    }

    @Override
    public Void process(Map.Entry<String, Map<String, Statistics>> season) {
        var seasonStats = season.getValue();
        if (seasonStats == null) {
            var stats = new HashMap<String, Statistics>();
            stats.put(name, createStatistics(playerGameData));
            season.setValue(stats);
            return null;
        }
        seasonStats.merge(name, createStatistics(playerGameData), MapEntryProcessor::mergeStatistics);
        season.setValue(seasonStats);
        return null;
    }

    static Statistics createStatistics(PlayerGameData playerGameData) {
        return new Statistics(
            new ContinuousAverage().addValue(playerGameData.getPoints()),
            new ContinuousAverage().addValue(playerGameData.getRebounds()),
            new ContinuousAverage().addValue(playerGameData.getAssists()),
            new ContinuousAverage().addValue(playerGameData.getSteals()),
            new ContinuousAverage().addValue(playerGameData.getBlocks()),
            new ContinuousAverage().addValue(playerGameData.getFouls()),
            new ContinuousAverage().addValue(playerGameData.getTurnovers()),
            new ContinuousAverage().addValue(playerGameData.getMinutesPlayed())
        );
    }

    static Statistics mergeStatistics(Statistics oldStats, Statistics newStats) {
        oldStats.avgPoints().addValue(newStats.avgPoints().average);
        oldStats.avgRebounds().addValue(newStats.avgRebounds().average);
        oldStats.avgAssists().addValue(newStats.avgAssists().average);
        oldStats.avgSteals().addValue(newStats.avgSteals().average);
        oldStats.avgBlocks().addValue(newStats.avgBlocks().average);
        oldStats.avgFouls().addValue(newStats.avgFouls().average);
        oldStats.avgTurnovers().addValue(newStats.avgTurnovers().average);
        oldStats.avgMinutesPlayed().addValue(newStats.avgMinutesPlayed().average);
        return oldStats;
    }

    @Override
    public String getExecutorName() {
        return "ExecutorOfMapEntryProcessor";
    }
}
