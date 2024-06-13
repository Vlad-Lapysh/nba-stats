package dev.lapysh.infra.serde;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dev.lapysh.core.model.ContinuousAverage;

public class ContinuousAverageSerializer implements CompactSerializer<ContinuousAverage> {

    @Override
    public ContinuousAverage read(CompactReader reader) {
        var n = reader.readInt64("n");
        var average = reader.readFloat64("average");
        return new ContinuousAverage(n, average);
    }

    @Override
    public void write(CompactWriter writer, ContinuousAverage continuousAverage) {
        writer.writeInt64("n", continuousAverage.n);
        writer.writeFloat64("average", continuousAverage.average);
    }

    @Override
    public Class<ContinuousAverage> getCompactClass() {
        return ContinuousAverage.class;
    }

    @Override
    public String getTypeName() {
        return "continuousAverage";
    }
}
