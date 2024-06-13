package dev.lapysh.core.model;

public record Statistics(ContinuousAverage avgPoints,
                         ContinuousAverage avgRebounds,
                         ContinuousAverage avgAssists,
                         ContinuousAverage avgSteals,
                         ContinuousAverage avgBlocks,
                         ContinuousAverage avgFouls,
                         ContinuousAverage avgTurnovers,
                         ContinuousAverage avgMinutesPlayed) {
}
