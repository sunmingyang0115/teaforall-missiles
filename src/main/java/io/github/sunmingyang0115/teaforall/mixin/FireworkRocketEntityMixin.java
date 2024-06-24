package io.github.sunmingyang0115.teaforall.mixin;

import com.sun.jna.platform.win32.OaIdl;
import net.minecraft.block.*;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {


    @Unique
    void spawnParticle(ServerPlayerEntity spe, Vec3d pos) {
        spe.getServerWorld().spawnParticles(spe, ParticleTypes.CLOUD, true, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0.1);
    }


    @Unique
    @Nullable LivingEntity getClosestLivingEntity(Vec3d pos, List<LivingEntity> entity_list, double d) {
        LivingEntity target = null;
        for (LivingEntity le : entity_list) {
            double nd = le.squaredDistanceTo(pos);
            if (nd < d) {
                d = nd;
                target = le;
            }
            else System.out.println(nd);
        }
        return target;
    }


    @Redirect(method = "explodeAndRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FireworkRocketEntity;explode()V"))
        public void explode(FireworkRocketEntity that) {
        /**
         * normal => 'normal explosion'
         * feather => 'armor piercing' : 1/2 damage + armor damage + block piercing
         * fire charge => 'high explosive' : huge radius
         * mob skull => 'huge damage' : huge damage?
         */
        List<FireworkExplosionComponent> list_fec = ((FireworkRocketEntityAccessor) that).invokeGetExplosions();
//        System.out.println(list_fec.size());
        if (list_fec.isEmpty()) return;
        FireworkExplosionComponent fec = list_fec.get(that.getRandom().nextBetween(0, list_fec.size()-1));
        int size_bonus_damage = list_fec.size();
        float base_damage = 5.0f + 2.0f * size_bonus_damage;

        List<LivingEntity> nearby_living_entities = that.getWorld().getNonSpectatingEntities(LivingEntity.class, that.getBoundingBox().expand(5.0));

        if (fec.shape() == FireworkExplosionComponent.Type.BURST) {
            for (LivingEntity le : nearby_living_entities) {
                float dist = that.distanceTo(le);
                if (dist >= 5) continue;
                float d = (float) (base_damage*Math.sqrt((5 - dist)/5));
                DamageSource ds = that.getDamageSources().fireworks(that, that.getOwner());
                le.damage(ds, d/2);
                ((LivingEntityAccessor) le).invokeDamageArmor(ds, d/2);
            }
        } else {
            if (that.getOwner() instanceof PlayerEntity p) {
                p.sendMessage(Text.of("<other_explosion!>"), true);
            }
        }



//        else if (fec.shape() == FireworkExplosionComponent.Type.LARGE_BALL) {
//            float f = 5.0f + size_bonus_damage;
//
//        }


        // ====

//        float f = 5.0f + (float)(list.size() * 2);
//
//        // choose a random effect from the list of fireworks to be applied
////        FireworkExplosionComponent fec = list.get(that.getRandom().nextBetween(0, list.size()-1));
//
//        if (f > 0.0f) {
//            double d = 5.0;
//            Vec3d vec3d = that.getPos();
//            List<LivingEntity> list2 = that.getWorld().getNonSpectatingEntities(LivingEntity.class, that.getBoundingBox().expand(5.0));
//            for (LivingEntity livingEntity : list2) {
//                boolean bl = false;
//                for (int i = 0; i < d; ++i) {
//                    Vec3d vec3d2 = new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5 * (double)i), livingEntity.getZ());
//                    BlockHitResult hitResult = that.getWorld().raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, that));
//                    if (((HitResult)hitResult).getType() != HitResult.Type.MISS) continue;
//                    bl = true;
//                    break;
//                }
//                if (!bl) continue;
//                float g = f * (float)Math.sqrt((5.0 - (double)that.distanceTo(livingEntity)) / 5.0);
//                livingEntity.damage(that.getDamageSources().fireworks(that, that.getOwner()), g);
//            }
//        }
//        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        FireworkRocketEntity that = (FireworkRocketEntity) (Object) this;

        // manually guided missile
        if (that == null || that.getOwner() == null || !that.wasShotAtAngle()) return;
//        ((FireworkRocketEntityAccessor) that).setLifetime(20);
        if (that.distanceTo(that.getOwner()) > 8*16) {
            ((FireworkRocketEntityAccessor) that).invokeExplodeAndRemove();
        }

        // make all fireworks experience inaccuracy
        if (that.age%4 == 0) {
            that.age += 2*(that.getRandom().nextGaussian()+1);  // 0 - 2
            Vec3d ndir_raw = that.getVelocity().normalize();
            Vec3d ran = new Vec3d(that.getRandom().nextGaussian(), that.getRandom().nextGaussian()-0.8, that.getRandom().nextGaussian());
            Vec3d ndir = ndir_raw.add(ran.multiply(0.05)).normalize();
            that.setVelocity(ndir.multiply(that.getVelocity().length()));
        }

//        that.getExplosions().get(0).

//        if (that.getOwner() instanceof PlayerEntity p) {
//            // relative tracking
//            if (p.isSneaking()) {
//                p.sendMessage(Text.of("Tracking..." + that.getPos().toString()), true);
//                Vec3d eye = that.getOwner().getRotationVector();
//                Vec3d ndir_raw = that.getVelocity().normalize().multiply(0.8).add(eye.normalize().multiply(0.2));
//                that.setVelocity(ndir_raw.multiply(1.6));
//            } else {
//                Vec3d ndir_raw = that.getVelocity().normalize();
//                Vec3d ran = new Vec3d(that.getRandom().nextGaussian(), that.getRandom().nextGaussian()-0.8, that.getRandom().nextGaussian());
//                Vec3d ndir = ndir_raw.add(ran.multiply(0.05)).normalize();
//                that.setVelocity(ndir.multiply(that.getVelocity().length()));
////                ((FireworkRocketEntityAccessor) that).invokeExplodeAndRemove();
//            }
//
//
//        }


        // artillery
//        Vec3d ndir_raw = that.getVelocity().normalize();
//        Vec3d ran = new Vec3d(that.getRandom().nextGaussian(), that.getRandom().nextGaussian()-0.8, that.getRandom().nextGaussian());
//        Vec3d ndir = ndir_raw.add(ran.multiply(0.02)).normalize();
//        that.setVelocity(ndir.multiply(that.getVelocity().length()));


    }

    @Unique
    private boolean livingEntityInRange(FireworkRocketEntity that, int d) {
        for (LivingEntity le : that.getWorld().getNonSpectatingEntities(LivingEntity.class, that.getBoundingBox().expand(5.0))) {
            if (le != that.getOwner() && that.distanceTo(le) <= d) return true;
        }
        return false;
    }

}

