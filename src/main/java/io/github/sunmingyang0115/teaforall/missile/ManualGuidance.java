package io.github.sunmingyang0115.teaforall.missile;

import net.minecraft.util.math.Vec3d;

public class ManualGuidance extends GuidedMissile{
    Vec3d shooterDir;

    public ManualGuidance(Vec3d projPos, Vec3d projVel, Vec3d shooterDir) {
        super(projPos, projVel);
        this.shooterDir = shooterDir;
    }

    @Override
    protected Vec3d calculateLeadingDir() {
        return shooterDir.normalize().multiply(0.2).add(this.getProjVel().normalize().multiply(0.8));
    }

    @Override
    public Vec3d getImpactPosition() {
        return null;
    }


}
