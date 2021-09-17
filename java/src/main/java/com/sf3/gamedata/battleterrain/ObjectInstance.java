package com.sf3.gamedata.battleterrain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf3.gamedata.sgl.EulerAngles;
import com.sf3.gamedata.sgl.Point;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class ObjectInstance {
    private final int meshId;
    private final EulerAngles rotation;
    private final Point position;
    private final Point scale;

    public ObjectInstance(int meshId, EulerAngles rotation, Point position, Point scale) {
        this.meshId = meshId;
        this.rotation = rotation;
        this.position = position;
        this.scale = scale;
    }

    @SneakyThrows
    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
