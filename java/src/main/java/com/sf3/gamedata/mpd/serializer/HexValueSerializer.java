package com.sf3.gamedata.mpd.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sf3.gamedata.utils.HexValue;

import java.io.IOException;

public class HexValueSerializer extends StdSerializer<HexValue> {

    public HexValueSerializer() {
        this(null);
    }

    public HexValueSerializer(Class<HexValue> t) {
        super(t);
    }

    @Override
    public void serialize(
            HexValue value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeString(value.toString());
    }
}
