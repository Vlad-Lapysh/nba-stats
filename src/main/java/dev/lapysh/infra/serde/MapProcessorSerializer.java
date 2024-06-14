package dev.lapysh.infra.serde;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dev.lapysh.in.model.PlayerGameData;
import dev.lapysh.in.model.PlayerGameDataSerializer;
import dev.lapysh.core.MapEntryProcessor;

public class MapProcessorSerializer implements CompactSerializer<MapEntryProcessor> {

    private final PlayerGameDataSerializer playerGameDataSerializer = new PlayerGameDataSerializer();

    @Override
    public MapEntryProcessor read(CompactReader reader) {
        String name = reader.readString("name");
        PlayerGameData playerGameData = playerGameDataSerializer.read(reader);
        return new MapEntryProcessor(name, playerGameData);
    }

    @Override
    public void write(CompactWriter writer, MapEntryProcessor object) {
        writer.writeString("name", object.name);
        playerGameDataSerializer.write(writer, object.playerGameData);
    }

    @Override
    public Class<MapEntryProcessor> getCompactClass() {
        return MapEntryProcessor.class;
    }

    @Override
    public String getTypeName() {
        return "dev.lapysh.service.MapEntryProcessor";
    }
}
