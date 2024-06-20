package io.github.sunmingyang0115.teaforall;

import net.minecraft.block.BlockState;
import net.minecraft.block.BulbBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface ScheduledTickAdder {
    default void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    }
}