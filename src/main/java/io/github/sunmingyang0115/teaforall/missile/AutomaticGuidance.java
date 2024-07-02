package io.github.sunmingyang0115.teaforall.missile;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Unique;

public class AutomaticGuidance extends GuidedMissile {
    Vec3d targetVel, targetPos;
    Vec3d impactPos;

    public AutomaticGuidance(Vec3d projPos, Vec3d projVel, Vec3d targetPos, Vec3d targetVel) {
        super(projPos, projVel);
        this.targetPos = targetPos;
        this.targetVel = targetVel;
        this.impactPos = null;
    }

    @Override
    protected Vec3d calculateLeadingDir() {
        double t = 0;
        Vec3d pred = null;
        for (int i = 0; i < 10; i++) {
            pred = targetPos.add(targetVel.multiply(t));
            t = pred.subtract(this.getProjPos()).length() / this.getProjVel().length();
        }
        impactPos = pred;
        return pred.subtract(this.getProjPos()).normalize();
    }

    @Override
    public Vec3d getImpactPosition() {
        return impactPos;
    }
}
