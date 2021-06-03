package gay.ampflower.helium.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(PlayerEntity.class)
public interface AccessorPlayerEntity {
    @Accessor
    PlayerInventory getInventory();
}
