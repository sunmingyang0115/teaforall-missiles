package io.github.sunmingyang0115.teaforall.missile;

import io.github.sunmingyang0115.teaforall.util.TagDB;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import static io.github.sunmingyang0115.teaforall.util.DBFlags.RWR_TYPE;

public class RWR {
    /**
     * Resolves RWR priority (0 < 1 < 2)
     * @param target the target receiving RWR
     * @param rwrType 0 for detecting tracking signal, 1 for during tracking, 2 for missile launched
     */
    public static void invokeRWR(Entity target, int rwrType) {
        if (target instanceof PlayerEntity) {
            TagDB t_db = new TagDB(target);
            if (!t_db.contains(RWR_TYPE) || t_db.getInt(RWR_TYPE) < rwrType) {
                t_db.putInt(RWR_TYPE, rwrType);
                t_db.write();
            }
        }

    }
}
