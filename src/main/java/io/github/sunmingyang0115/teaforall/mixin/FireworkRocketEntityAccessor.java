package io.github.sunmingyang0115.teaforall.mixin;

import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("lifeTime")
    public void setLifetime(int lifeTime);

    @Invoker("explodeAndRemove")
    public void invokeExplodeAndRemove();

    @Invoker("getExplosions")
    public List<FireworkExplosionComponent> invokeGetExplosions();
}
