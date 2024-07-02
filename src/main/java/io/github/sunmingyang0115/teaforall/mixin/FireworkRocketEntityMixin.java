package io.github.sunmingyang0115.teaforall.mixin;

import io.github.sunmingyang0115.teaforall.missile.AutomaticGuidance;
import io.github.sunmingyang0115.teaforall.missile.GuidedMissile;
import io.github.sunmingyang0115.teaforall.missile.ManualGuidance;
import io.github.sunmingyang0115.teaforall.util.TagDB;
import io.github.sunmingyang0115.teaforall.util.Vec3dExtraUtil;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.StructureSpawns;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.sunmingyang0115.teaforall.util.DBFlags.*;


@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {
    @Unique
    private static final double MAX_TURN = (5)*Math.PI/180;


    @Unique
    public LivingEntity pemAttemptTrack(PlayerEntity that, double size, double radius) {
        LivingEntity e = null;
        Vec3d dir = that.getRotationVector();
        Vec3d pos1 = that.getEyePos();
        for (LivingEntity le : that.getWorld().getEntitiesByClass(LivingEntity.class, that.getBoundingBox().expand(size), EntityPredicates.VALID_LIVING_ENTITY)) {
            if (le.equals(that)) continue;
            for (Vec3d pos2 : new Vec3d[]{le.getPos(), le.getEyePos()}) {
                double na = Math.acos( pos2.subtract(pos1).normalize().dotProduct(dir) );
                if (radius == -1 || radius > na) {
                    BlockHitResult br = that.getWorld().raycast(new RaycastContext(pos1, pos2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, that));
                    if (((HitResult)br).getType() == HitResult.Type.MISS) {
                        e = le;
                        radius = na;
                    }
                }
            }
        }
        return e;
    }

    @Unique
    public double predictSplashTime(Vec3d pos1, double vel1, Vec3d pos2, Vec3d vel2) {
        double t = 0; // initial guess
        Vec3d pred = null;
        double[] ts = new double[10];
        for (int i = 0; i < 10; i++) {
            pred = pos2.add(vel2.multiply(t));
            t = pred.subtract(pos1).length()/vel1;
//            ts[i] = t;
        }
//        System.out.println(Arrays.toString(ts));
        return t;
    }

    @Unique
    public Vec3d predictPNSplashTime(Vec3d pos1, double vel1, Vec3d pos2, Vec3d vel2) {
        // use pi/4 as PN angle
        Vec3d dpos = pos2.subtract(pos1);
        Vec3d _vel1 = Vec3dExtraUtil.rotateInDirection(vel2, dpos, Math.PI/4.0).normalize().multiply(vel1);
//        double t = Vec3dExtraUtil.perp(_vel1, vel2).length() / dpos.length();
//        System.out.println(t);
//        return t;
        return _vel1;
    }



    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {


        FireworkRocketEntity that = (FireworkRocketEntity) (Object) this;
        FireworkRocketEntityAccessor acc = ((FireworkRocketEntityAccessor) that);
        TagDB db = new TagDB(that);

        // manually guided missile
        if (that == null || that.getOwner() == null || !that.wasShotAtAngle()) return;

        // reduce lag
        if (that.distanceTo(that.getOwner()) > 8*16) {
            acc.invokeExplodeAndRemove();
        }

        List<FireworkExplosionComponent> list_fec = ((FireworkRocketEntityAccessor) that).invokeGetExplosions();
        FireworkExplosionComponent fec = list_fec.size()==0?null:list_fec.get(0);



        // make guided missiles last longer
        if (fec != null && that.age == 0) {
            if (fec.hasTrail() || fec.hasTwinkle()) {
                acc.setLifetime(acc.getLifeTime() * 4);
            }
        }

        // tracking
        if (fec != null && that.age < 5) {
            if (!db.contains(TRACKING_ID) && fec.hasTrail() && that.getOwner() instanceof PlayerEntity p) {
                TagDB pdb = new TagDB(p);
                if (pdb.contains(TRACKING_TIME) && pdb.getInt(TRACKING_TIME) == 0) {
                    db.putInt(TRACKING_ID, pdb.getInt(TRACKING_ID));
                }
            }
        }


        if (db.contains(FUSE)) {
            int f = db.getInt(FUSE);
            if (f == 0) {
                Vec3d p = db.getVec3d(DETONATION_POS);
                that.setPos(p.x, p.y, p.z);
                ((FireworkRocketEntityAccessor) that).invokeExplodeAndRemove();
            }
            db.putInt(FUSE, f-1);
        }

        Vec3d nvel;

        if (fec != null && that.getOwner() instanceof PlayerEntity p && db.contains(TRACKING_ID)) {
            LivingEntity tracker = (LivingEntity) that.getWorld().getEntityById(db.getInt(TRACKING_ID));
            if (tracker != null) {
                GuidedMissile gm = new AutomaticGuidance(that.getBoundingBox().getCenter(), that.getVelocity(), tracker.getBoundingBox().getCenter(), tracker.getVelocity());

                nvel = gm.getGeeLimitedLeadingDir().multiply(2);
                int t = gm.getFuse();
                if (t != -1 && !db.contains(FUSE)) {
                    db.putInt(FUSE, t);
                    db.putVec3d(DETONATION_POS, gm.getImpactPosition());
                }

                if (that.age == 10 && tracker instanceof PlayerEntity p2) {
                    p2.playSoundToPlayer(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1, 1);
                }

            } else {
                nvel = randomizeVel(that, 0.05);
            }
        }
        else if (fec != null && that.getOwner() instanceof PlayerEntity p && (fec.hasTwinkle() || fec.hasTrail())) {
            Vec3d eye = p.getRotationVector();
            Vec3d ndir_raw = that.getVelocity().normalize().multiply(0.8).add(eye.normalize().multiply(0.2));
            nvel = ndir_raw.multiply(2);
        }
        else {
            nvel = randomizeVel(that, 0.05);
        }
//        nvel = randomizeVel(that, 0.05);



//        // particles
//        for (PlayerEntity p : that.getWorld().getPlayers()) {
//            if (p.distanceTo(that) > 100) return;
//            ServerPlayerEntity spe = (ServerPlayerEntity) p;
//
//            if (that.age%2==0) {
//                spe.getServerWorld().spawnParticles(spe, ParticleTypes.GUST, true, that.getPos().getX(), that.getPos().getY(), that.getPos().getZ(), 1,0, 0, 0, 0.01);
//            }
//        }

        if (Vec3dExtraUtil.getAngle(that.getVelocity(), nvel) > MAX_TURN) {

            that.setVelocity(Vec3dExtraUtil.rotateInDirection(that.getVelocity(), nvel, MAX_TURN));

        }else {
//            spe.getServerWorld().spawnParticles(spe, ParticleTypes.HEART, true, that.getPos().getX(), that.getPos().getY(), that.getPos().getZ(), 1, 0, 0, 0, 0.01);
            that.setVelocity(nvel);
        }
//        that.setVelocity(randomizeVel(that, 0.05));





        db.write();


//        ProjectileUtil.setRotationFromVelocity(that, 0.2f);

//        // prox
//        boolean flag = false;
//        if (that.age > 10) {
//            for (LivingEntity le : that.getWorld().getEntitiesByClass(LivingEntity.class, that.getBoundingBox().expand(5), EntityPredicates.VALID_LIVING_ENTITY)) {
//                for (int i = 0; i < 8; i++) {
//                    Vec3d pos2 = Vec3dExtraUtil.lerp(le.getPos(), le.getPos().add(le.getVelocity()), i/8.0);
//                    for (int j = 0; j < 8; j++) {
//                        Vec3d pos1 = Vec3dExtraUtil.lerp(that.getPos(), that.getPos().add(that.getVelocity()), i/8.0);
//                        if (pos2.subtract(pos1).length() <= 2) {
//                            flag = true;
////                            le.sendMessage(Text.of("dog"));
//                        }
//                    }
//                }
//            }
//        }
//
//
//        if (flag) ((FireworkRocketEntityAccessor) that).invokeExplodeAndRemove();


//        that.setVelocity(Vec3dExtraUtil.getAngle(that.getVelocity(), nvel) > MAX_TURN?Vec3dExtraUtil.rotateInDirection(that.getVelocity(), nvel, MAX_TURN):nvel);


//        TagDB db = new TagDB(that);
//        if (!db.contains(G_TOLERANCE)) {
//            db.putDouble(G_TOLERANCE, 3);
//        }
//        double db_angle = db.getDouble(G_TOLERANCE);
//        double angle = Math.acos(nvel.normalize().dotProduct(that.getVelocity().normalize()));
//        if (angle < db_angle) {
//            that.setVelocity(nvel);
//            db_angle -= angle;
//        }db_angle += 0.05;
//        db.putDouble(G_TOLERANCE, db_angle);
//        db.write();

    }

    @Unique
    private Vec3d randomizeVel(FireworkRocketEntity that, double alpha) {
        if (that.age%4 == 0) {
            that.age += 2*(that.getRandom().nextGaussian()+1);  // 0 - 2
            Vec3d ndir_raw = that.getVelocity().normalize();
            Vec3d ran = new Vec3d(that.getRandom().nextGaussian(), that.getRandom().nextGaussian()-0.8, that.getRandom().nextGaussian());
            Vec3d ndir = ndir_raw.add(ran.multiply(alpha)).normalize();
            return ndir.multiply(that.getVelocity().length());
        }
        return that.getVelocity();
    }

}

