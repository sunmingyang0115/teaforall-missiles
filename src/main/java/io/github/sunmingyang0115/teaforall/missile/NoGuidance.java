package io.github.sunmingyang0115.teaforall.missile;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class NoGuidance extends GuidedMissile {
    Random ran;
    int age;
    float alpha;
    public NoGuidance(Vec3d projPos, Vec3d projVel, Random r, int age, float alpha) {
        super(projPos, projVel);
        this.alpha = alpha;
        this.ran = r;
        this.age = age;
    }

    @Override
    protected Vec3d calculateLeadingDir() {
        if (age % 4 != 0) return getProjVel().normalize();
        Vec3d ndir_raw = getProjVel().normalize();
        Vec3d ran = new Vec3d(this.ran.nextGaussian(), this.ran.nextGaussian()-0.8, this.ran.nextGaussian());
        Vec3d ndir = ndir_raw.add(ran.multiply(alpha)).normalize();
        return ndir;
    }

    @Override
    public Vec3d getImpactPosition() {
        return null;
    }
}
