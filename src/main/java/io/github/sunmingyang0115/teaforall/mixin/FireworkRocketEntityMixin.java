package io.github.sunmingyang0115.teaforall.mixin;

import io.github.sunmingyang0115.teaforall.missile.AutomaticGuidance;
import io.github.sunmingyang0115.teaforall.missile.GuidedMissile;
import io.github.sunmingyang0115.teaforall.missile.ManualGuidance;
import io.github.sunmingyang0115.teaforall.missile.NoGuidance;
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

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {


        FireworkRocketEntity that = (FireworkRocketEntity) (Object) this;
        FireworkRocketEntityAccessor acc = ((FireworkRocketEntityAccessor) that);
        TagDB db = new TagDB(that);

        if (that == null || that.getOwner() == null || !that.wasShotAtAngle()) return;

        // reduce lag
        if (that.distanceTo(that.getOwner()) > 8 * 16) {
            acc.invokeExplodeAndRemove();
        }

        List<FireworkExplosionComponent> list_fec = ((FireworkRocketEntityAccessor) that).invokeGetExplosions();
        FireworkExplosionComponent fec = list_fec.size() == 0 ? null : list_fec.get(0);


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
            db.putInt(FUSE, f - 1);
        }

        Vec3d nvel;

        if (fec != null && that.getOwner() instanceof PlayerEntity p && db.contains(TRACKING_ID)) {
            LivingEntity tracker = (LivingEntity) that.getWorld().getEntityById(db.getInt(TRACKING_ID));
            if (tracker != null) {
                GuidedMissile gm = new AutomaticGuidance(that.getBoundingBox().getCenter(), that.getVelocity(), tracker.getBoundingBox().getCenter(), tracker.getVelocity());
                nvel = gm.getGeeLimitedLeadingDir();
                int t = gm.getFuse();
                if (t != -1 && !db.contains(FUSE)) {
                    db.putInt(FUSE, t);
                    db.putVec3d(DETONATION_POS, gm.getImpactPosition());
                }

                // rwr
                double d = that.getPos().distanceTo(tracker.getPos());
                if (tracker instanceof PlayerEntity tracker_p) {
                    TagDB tdb = new TagDB(tracker_p);
                    if (!tdb.contains(CLOSEST_MISSILE) || tdb.getDouble(CLOSEST_MISSILE) > d) {
                        tdb.putDouble(CLOSEST_MISSILE, d);
                    }
                    tdb.write();
                }

            } else {
                nvel = new NoGuidance(that.getBoundingBox().getCenter(), that.getVelocity(), that.getRandom(), that.age, 0.05f).getGeeLimitedLeadingDir();
            }
        } else if (fec != null && that.getOwner() instanceof PlayerEntity p && (fec.hasTwinkle() || fec.hasTrail())) {
            nvel = new ManualGuidance(that.getBoundingBox().getCenter(), that.getVelocity(), p.getRotationVector()).getGeeLimitedLeadingDir();
        } else {
            nvel = new NoGuidance(that.getBoundingBox().getCenter(), that.getVelocity(), that.getRandom(), that.age, 0.05f).getGeeLimitedLeadingDir();
        }
        that.setVelocity(nvel.multiply(2));
//        System.out.println("bruh " + nvel.multiply(2));
        db.write();
    }




}

