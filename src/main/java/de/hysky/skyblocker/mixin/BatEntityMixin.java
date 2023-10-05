package de.hysky.skyblocker.mixin;

import de.hysky.skyblocker.skyblock.dungeon.secrets.DungeonSecrets;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BatEntity.class)
public abstract class BatEntityMixin extends AmbientEntity {
    protected BatEntityMixin(EntityType<? extends AmbientEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        DungeonSecrets.onBatRemoved(this);
    }
}
