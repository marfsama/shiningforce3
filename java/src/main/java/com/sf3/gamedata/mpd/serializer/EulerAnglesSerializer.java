package com.sf3.gamedata.mpd.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sf3.gamedata.sgl.EulerAngles;
import com.sf3.gamedata.sgl.Point;

import java.io.IOException;

public class EulerAnglesSerializer extends StdSerializer<EulerAngles> {
    public EulerAnglesSerializer() {
        this(null);
    }

    public EulerAnglesSerializer(Class<EulerAngles> t) {
        super(t);
    }

    @Override
    public void serialize(
            EulerAngles value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeStartArray();
        jgen.writeNumber(value.getX().getRadians());
        jgen.writeNumber(value.getY().getRadians());
        jgen.writeNumber(value.getZ().getRadians());
        jgen.writeEndArray();
    }
}
