package com.sf3.gamedata.sgl;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/** Rotation around an axis. Not an official SGL class. */
public class RotationAxis {
    private final Fixed x;
    private final Fixed y;
    private final Fixed z;
    private final Angle theta;

    public RotationAxis(ImageInputStream stream) throws IOException {
        this(new Fixed(stream), new Fixed(stream), new Fixed(stream), new Angle(stream));
    }
    public RotationAxis(Fixed x, Fixed y, Fixed z, Angle theta) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.theta = theta;
    }

    @Override
    public String toString() {
        return "\"[ " + x + " , " + y + " , " + z + ", " + theta.getRadians() + " ]\"";
    }
}
