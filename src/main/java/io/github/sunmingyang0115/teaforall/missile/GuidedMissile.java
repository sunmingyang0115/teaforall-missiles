package io.github.sunmingyang0115.teaforall.missile;


import io.github.sunmingyang0115.teaforall.util.Vec3dExtraUtil;
import net.minecraft.util.math.Vec3d;

public abstract class GuidedMissile {
    static int PROXIMITY_THRESHOLD = 5;
    static double MAX_TURN_ANGLE = 0.087;
    Vec3d projVel, projPos;
    public GuidedMissile(Vec3d projPos, Vec3d projVel) {
        this.projPos = projPos;
        this.projVel = projVel;
    }

    public Vec3d getGeeLimitedLeadingDir() {
        Vec3d nvel = this.calculateLeadingDir();
        if (Vec3dExtraUtil.getAngle(projVel, nvel) > MAX_TURN_ANGLE)
            return Vec3dExtraUtil.rotateInDirection(projVel.normalize(), nvel, MAX_TURN_ANGLE);
        return this.calculateLeadingDir();
    }

    /**
     * Gets the fuse in ticks, representing how many ticks until projectile should explode
     * @return the fuse, or -1 if no fuse (should not explode yet)
     */
    public int getFuse() {
        Vec3d impact = getImpactPosition();
        if (impact == null) return -1;
        double t = getProjPos().subtract(impact).length() / getProjVel().length();
        if (t > PROXIMITY_THRESHOLD) return -1;
        return (int)t;
    }

    /**
     * How much the projectile should lead
     * @return the direction
     */
    protected abstract Vec3d calculateLeadingDir();
    public abstract Vec3d getImpactPosition();

    protected Vec3d getProjVel() {
        return projVel;
    }
    protected Vec3d getProjPos() {
        return projPos;
    }
}
