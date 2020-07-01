package gay.ampflower.plymouth.mixins;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerWorld.class)
public interface AccessorServerWorld {
    @Accessor("field_24456")
    ServerWorldProperties getWorldProperties();
}
