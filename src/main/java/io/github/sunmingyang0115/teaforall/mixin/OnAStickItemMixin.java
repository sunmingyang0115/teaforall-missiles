package io.github.sunmingyang0115.teaforall.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.OnAStickItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

@Mixin(OnAStickItem.class)
public class OnAStickItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    public void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> ci) {
        ItemStack itemStack = user.getStackInHand(hand);
        String cmd = itemStack.getName().getString();
        System.out.println(cmd);
        if (itemStack.getName().getString().equals("clearTags")) {
            String[] tags = user.getCommandTags().toArray(String[]::new);
            for (String tag : tags) {
                user.removeCommandTag(tag);
            }
        }
    }
}
