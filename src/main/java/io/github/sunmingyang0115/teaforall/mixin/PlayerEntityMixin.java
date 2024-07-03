package io.github.sunmingyang0115.teaforall.mixin;

import io.github.sunmingyang0115.teaforall.util.TagDB;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.PlaySoundCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import static io.github.sunmingyang0115.teaforall.util.DBFlags.*;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
//    Vec3d getPredictedPath(double vel, Vec3d s_pos, Vec3d t_pos, Vec3d t_vel) {
////        return  t_pos;
////        double rel_vel = t_vel.length();
//        double dist = s_pos.distanceTo(t_pos);
//        double t = dist/vel;    // approx time in ticks to splash target
//
//        Vec3d pred_pos = t_pos.add(t_vel.multiply(t));
//        return pred_pos;
//    }
//    List<PlayerEntity> getPlayersInVicinity(PlayerEntity anchor, float dist) {
//        ArrayList<PlayerEntity> players = new ArrayList<>();
//        for (PlayerEntity p : anchor.getWorld().getPlayers()) {
//            if (anchor.distanceTo(p) < dist && p != anchor) players.add(p);
//        }
//        return players;
//    }

//    @Unique
//    public LivingEntity pemAttemptTrack(PlayerEntity that, double size, double radius) {
//        LivingEntity e = null;
//        Vec3d dir = that.getRotationVector();
//        Vec3d pos1 = that.getEyePos();
//        for (LivingEntity le : that.getWorld().getEntitiesByClass(LivingEntity.class, that.getBoundingBox().expand(size), EntityPredicates.VALID_LIVING_ENTITY)) {
//            if (le == that) continue;
//            for (Vec3d pos2 : new Vec3d[]{le.getPos(), le.getEyePos()}) {
//                double na = Math.acos( pos2.subtract(pos1).normalize().dotProduct(dir) );
//                if (radius == -1 || radius > na) {
//                    BlockHitResult br = that.getWorld().raycast(new RaycastContext(pos1, pos2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, that));
//                    if (((HitResult)br).getType() == HitResult.Type.MISS) {
//                        e = le;
//                        radius = na;
//                    }
//                }
//            }
//
//
//        }
//
//        return e;
//    }

    @Unique
    private static LivingEntity getTrackingCandidate(PlayerEntity that, double size, double radius) {
        LivingEntity e = null;
        Vec3d dir = that.getRotationVector();
        Vec3d pos1 = that.getEyePos();
//        HashMap<LivingEntity, Double> candidates = new HashMap<>();

        for (LivingEntity le : that.getWorld().getEntitiesByClass(LivingEntity.class, that.getBoundingBox().expand(size), EntityPredicates.VALID_LIVING_ENTITY)) {
            if (le == that || le.distanceTo(that) > 100) continue;
            for (Vec3d pos2 : new Vec3d[]{le.getPos(), le.getEyePos()}) {
                double na = Math.acos( pos2.subtract(pos1).normalize().dotProduct(dir) );
                if (radius > na) {
                    BlockHitResult br = that.getWorld().raycast(new RaycastContext(pos1, pos2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, that));
                    if (((HitResult)br).getType() == HitResult.Type.MISS) {
//                        candidates.put(e, na);
                        e = le;
                        radius = na;
                    }
                }
            }
        }
        return e;
    }


    @Unique
    void playSignal(SoundEvent sound, PlayerEntity p, float vol, float pitch) {
        ServerPlayerEntity spe = (ServerPlayerEntity) p;
        spe.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(sound), SoundCategory.PLAYERS, spe.getX(), spe.getY(), spe.getZ(), vol, pitch, spe.getRandom().nextLong()));
    }

    @Unique
    void playTrackInterruption(PlayerEntity p) {
        playSignal(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), p, 1, 0.3f);
        p.sendMessage(Text.of("Lock Interrupted"), true);
    }
    @Unique
    void playTrackProgress(PlayerEntity p) {
        if (p.getWorld().getTime() % 3 == 0) {
            playSignal(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), p, 1, 2f);
            p.sendMessage(Text.of("Tracking..."), true);
        }
    }
    @Unique
    void playTrackingLock(PlayerEntity p) {
        playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 1.95f);
        playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p,  0.33f, 2f);
        playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 1.6f);
        p.sendMessage(Text.of("Target Locked"), true);
    }

    @Unique
    void playRWR(PlayerEntity p, double d) {
        playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 1f);
    }


    @Unique
    private static final int TIME_NEEDED_FOR_TRACK = 20;
    @Unique
    private static final int WOUND_UP_RATE = 1;
    @Unique
    private static final int COOL_DOWN_RATE = 2;

    @Unique
    private static void increment(TagDB db, String value, int amount) {
        db.putInt(value, db.getInt(value) + amount);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity that = (PlayerEntity) (Object) this;
        ServerPlayerEntity spe = (ServerPlayerEntity) that;
        TagDB db = new TagDB(that);

        // rwr
        if (db.contains(CLOSEST_MISSILE) && that.age % 4 == 0) {
            playRWR(that, db.getDouble(CLOSEST_MISSILE));
            db.removeDouble(CLOSEST_MISSILE);
        }

        if (!db.contains(TRACKING_TIME) || !db.contains(TRACKING_ID)) {
            db.putInt(TRACKING_TIME, TIME_NEEDED_FOR_TRACK);
            db.putInt(TRACKING_ID, 0);
        }

        int time = db.getInt(TRACKING_TIME);
        int id = db.getInt(TRACKING_ID);

        if (!holdingTrackItem(that) && !that.isSneaking()) {
            // reset if switched off
            db.putInt(TRACKING_TIME, TIME_NEEDED_FOR_TRACK);
            db.write();
            return;
        }

        LivingEntity track = getTrackingCandidate(that, 100, Math.PI/22);


        if (track == null) {
            if (time == 0)
                playTrackInterruption(that);
            time = Math.min(TIME_NEEDED_FOR_TRACK, time + COOL_DOWN_RATE);
        } else if (track.getId() == id) {
            if (time - WOUND_UP_RATE == 0) {
                playTrackingLock(that);
            } else if (time != 0) {
                playTrackProgress(that);
            }
            time = Math.max(0, time - WOUND_UP_RATE);
        } else if (time == TIME_NEEDED_FOR_TRACK) {
            id = track.getId();
            time = TIME_NEEDED_FOR_TRACK - WOUND_UP_RATE;
        } else {
            if (time == 0)
                playTrackInterruption(that);
            time = Math.min(TIME_NEEDED_FOR_TRACK, time + COOL_DOWN_RATE);
        }


        db.putInt(TRACKING_TIME, time);
        db.putInt(TRACKING_ID, id);

//        that.sendMessage(Text.of(time + " " + id + " ; " + track), true);
        db.write();
    }

    @Unique
    private boolean holdingTrackItem(PlayerEntity p) {
        for (ItemStack is : p.getHandItems()) {
            ChargedProjectilesComponent cpc = is.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (cpc != null && !cpc.isEmpty() && cpc.getProjectiles().get(0).getItem() instanceof FireworkRocketItem fri) {
                FireworksComponent fc = cpc.getProjectiles().get(0).getComponents().get(DataComponentTypes.FIREWORKS);
                if (fc != null && !fc.explosions().isEmpty() && fc.explosions().get(0).hasTrail()) {
                    return true;
                }
            }
        }
        return false;
    }


}
