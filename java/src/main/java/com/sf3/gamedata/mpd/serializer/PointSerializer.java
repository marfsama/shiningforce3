package com.sf3.gamedata.mpd.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.utils.HexValue;

import java.io.IOException;

public class PointSerializer extends StdSerializer<Point> {
    public PointSerializer() {
        this(null);
    }

    public PointSerializer(Class<Point> t) {
        super(t);
    }

    @Override
    public void serialize(
            Point value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeStartArray();
        jgen.writeNumber(value.getX().toFloat());
        jgen.writeNumber(value.getY().toFloat());
        jgen.writeNumber(value.getZ().toFloat());
        jgen.writeEndArray();
    }
}
