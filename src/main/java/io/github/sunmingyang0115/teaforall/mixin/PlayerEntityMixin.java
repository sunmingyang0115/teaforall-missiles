package io.github.sunmingyang0115.teaforall.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    Vec3d getPredictedPath(double vel, Vec3d s_pos, Vec3d t_pos, Vec3d t_vel) {
//        return  t_pos;
//        double rel_vel = t_vel.length();
        double dist = s_pos.distanceTo(t_pos);
        double t = dist/vel;    // approx time in ticks to splash target
        
        Vec3d pred_pos = t_pos.add(t_vel.multiply(t));
        return pred_pos;
    }
    List<PlayerEntity> getPlayersInVicinity(PlayerEntity anchor, float dist) {
        ArrayList<PlayerEntity> players = new ArrayList<>();
        for (PlayerEntity p : anchor.getWorld().getPlayers()) {
            if (anchor.distanceTo(p) < dist && p != anchor) players.add(p);
        }
        return players;
    }
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {

        PlayerEntity that = (PlayerEntity) (Object) this;



//        List<PlayerEntity> players = getPlayersInVicinity(that, 100);
//        for (PlayerEntity p : players) {
////            Vec3d pred = getPredictedPath(1.6, that.getPos(), p.getPos(), p.getVelocity());
//            ServerPlayerEntity spe = (ServerPlayerEntity) that;
////            System.out.println(that.getName() + ": " + p.getName() + " " + pred);
//            spe.getServerWorld().spawnParticles(spe, ParticleTypes.CLOUD, true, spe.getX(), spe.getY(), spe.getZ(), 1, 0, 0, 0, 0.01);
//        }
//        System.out.println(players);
//        that.sendMessage(Text.of(that.raycast(10, 1, true).getPos().toString()), true);

    }
}
