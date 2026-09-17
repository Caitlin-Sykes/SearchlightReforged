package com.csykes.searchlight;

import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;

@AllArgsConstructor
public class MutableVector3d {
    public double x, y, z;

    public void add(Vec3 vector) {
        this.x += vector.x;
        this.y += vector.y;
        this.z += vector.z;
    }
}
