package io.github.sunmingyang0115.teaforall.util;

import net.minecraft.util.math.Vec3d;

public class Vec3dExtraUtil {
    public static Vec3d rotateInDirection(Vec3d vec, Vec3d target, double angle) {
        Vec3d cross = vec.crossProduct(target).crossProduct(vec).normalize();
        return vec.normalize().multiply(Math.cos(angle)).add(cross.multiply(Math.sin(angle))).multiply(vec.length());
    }

    public static double getAngle(Vec3d v1, Vec3d v2) {
        return Math.acos(v1.normalize().dotProduct(v2.normalize()));
    }

    public static Vec3d lerp(Vec3d v1, Vec3d v2, double t) {
        return v1.multiply(t).add(v2.multiply(1-t));
    }

    public static Vec3d proj(Vec3d v1, Vec3d v2) {
        return v2.normalize().multiply(v1.dotProduct(v2) / v2.length());
    }

    public static Vec3d perp(Vec3d v1, Vec3d v2) {
        return v2.subtract(proj(v1,v2));
    }
}
