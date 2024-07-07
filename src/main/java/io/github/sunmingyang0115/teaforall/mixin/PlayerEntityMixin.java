package io.github.sunmingyang0115.teaforall.mixin;

import io.github.sunmingyang0115.teaforall.util.TagDB;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
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
import net.minecraft.world.World;
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

import static io.github.sunmingyang0115.teaforall.missile.RWR.invokeRWR;
import static io.github.sunmingyang0115.teaforall.util.DBFlags.*;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Unique
    private static boolean hasLineOfSight(LivingEntity a, Vec3d loc) {
        BlockHitResult br = a.getWorld().raycast(new RaycastContext(a.getEyePos(), loc, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, a));
        return br.getType() == HitResult.Type.MISS;
    }

    @Unique
    private static LivingEntity getTrackingCandidate(PlayerEntity that, double size, double radius) {
        LivingEntity e = null;
        Vec3d dir = that.getRotationVector();
        Vec3d pos1 = that.getEyePos();

        for (LivingEntity le : that.getWorld().getEntitiesByClass(LivingEntity.class, that.getBoundingBox().expand(size), EntityPredicates.VALID_LIVING_ENTITY)) {
            if (le == that || le.distanceTo(that) > 100) continue;
            for (Vec3d pos2 : new Vec3d[]{le.getPos(), le.getEyePos()}) {
                double na = Math.acos( pos2.subtract(pos1).normalize().dotProduct(dir) );
                if (radius > na && hasLineOfSight(that, pos2)) {
                    e = le;
                    radius = na;
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
    void playRWR(PlayerEntity p, int t) {
        if (t == 0 && p.age % 20 == 0) {
            playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 2f);
        } else if (t == 1 && p.age % 5 == 0) {
            playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 0.3f);
        } else if (t == 2 && p.age % 4 == 0) {
            playSignal(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), p, 0.33f, 2f);
        }

    }

    @Unique
    private static final int TIME_NEEDED_FOR_TRACK = 10;
    @Unique
    private static final int WOUND_UP_RATE = 1;
    @Unique
    private static final int COOL_DOWN_RATE = 2;


    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity that = (PlayerEntity) (Object) this;


        ServerPlayerEntity spe = (ServerPlayerEntity) that;
        TagDB db = new TagDB(that);

        // rwr
        if (db.contains(RWR_TYPE)) {
            if (that.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof ArmorItem ai) {
                String name = ai.getName().getString();
                if (name.equals("Turtle Shell")) {
                    playRWR(that, db.getInt(RWR_TYPE));
                }
            }
            db.removeInt(RWR_TYPE);
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

        // rwr
        for (PlayerEntity p : that.getWorld().getPlayers()) {
            if (p.distanceTo(that) < 100) {
                invokeRWR(p, 0);
            }
        }

        LivingEntity track = getTrackingCandidate(that, 100, Math.PI/22);


        if (track == null) {
            if (time == 0)
                playTrackInterruption(that);
            time = Math.min(TIME_NEEDED_FOR_TRACK, time + COOL_DOWN_RATE);


        } else if (track.getId() == id) {
            // rwr
            invokeRWR(track, 1);
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
