package dev.lapysh.in.model;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

import java.util.UUID;

public class PlayerGameDataSerializer implements CompactSerializer<PlayerGameData> {

    @Override
    public PlayerGameData read(CompactReader reader) {
        var playerName = reader.readString("playerName");
        var teamName = reader.readString("teamName");
        var seasonName = reader.readString("seasonName");
        var gameId = reader.readString("gameId");
        var points = reader.readInt32("points");
        var rebounds = reader.readInt32("rebounds");
        var assists = reader.readInt32("assists");
        var steals = reader.readInt32("steals");
        var blocks = reader.readInt32("blocks");
        var turnovers = reader.readInt32("turnovers");
        var fouls = reader.readInt32("fouls");
        var minutesPlayed = reader.readFloat32("minutesPlayed");

        return new PlayerGameData(
            null, playerName, teamName, seasonName, UUID.fromString(gameId), points, rebounds, assists,
            steals, blocks, turnovers, fouls, minutesPlayed
        );
    }

    @Override
    public void write(CompactWriter writer, PlayerGameData playerGameData) {
        writer.writeString("playerName", playerGameData.getPlayerName());
        writer.writeString("teamName", playerGameData.getTeamName());
        writer.writeString("seasonName", playerGameData.getSeasonName());
        writer.writeString("gameId", playerGameData.getGameId().toString());
        writer.writeInt32("points", playerGameData.getPoints());
        writer.writeInt32("rebounds", playerGameData.getRebounds());
        writer.writeInt32("assists", playerGameData.getAssists());
        writer.writeInt32("steals", playerGameData.getSteals());
        writer.writeInt32("blocks", playerGameData.getBlocks());
        writer.writeInt32("turnovers", playerGameData.getTurnovers());
        writer.writeInt32("fouls", playerGameData.getFouls());
        writer.writeFloat32("minutesPlayed", playerGameData.getMinutesPlayed());
    }

    @Override
    public Class<PlayerGameData> getCompactClass() {
        return PlayerGameData.class;
    }

    @Override
    public String getTypeName() {
        return "playerGameData";
    }
}
