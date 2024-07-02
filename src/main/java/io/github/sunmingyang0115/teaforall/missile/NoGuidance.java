package io.github.sunmingyang0115.teaforall.missile;

import net.minecraft.util.math.Vec3d;

public class NoGuidance extends GuidedMissile {
    public NoGuidance(Vec3d projPos, Vec3d projVel) {
        super(projPos, projVel);
    }

    @Override
    protected Vec3d calculateLeadingDir() {
        return null;
    }

    @Override
    public Vec3d getImpactPosition() {
        return null;
    }
}
