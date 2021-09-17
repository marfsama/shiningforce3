package com.sf3.gamedata.sgl;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sf3.gamedata.mpd.serializer.EulerAnglesSerializer;
import com.sf3.gamedata.mpd.serializer.PointSerializer;
import lombok.Getter;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

@Getter
@JsonSerialize(using = EulerAnglesSerializer.class)
public class EulerAngles {
    private final Angle x;
    private final Angle y;
    private final Angle z;

    public EulerAngles(ImageInputStream stream) throws IOException {
        this.x = new Angle(stream.readUnsignedShort());
        this.y = new Angle(stream.readUnsignedShort());
        this.z = new Angle(stream.readUnsignedShort());
    }

    public EulerAngles(Angle x, Angle y, Angle z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
