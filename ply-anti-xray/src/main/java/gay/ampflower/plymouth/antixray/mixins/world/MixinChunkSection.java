package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.CloneAccessible;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.*;

/**
 * Forces the chunk section to be cloneable.
 *
 * @author Ampflower
 * @since 0.0.0
 */
@Mixin(ChunkSection.class)
public class MixinChunkSection implements Cloneable, CloneAccessible {
    @Shadow
    @Final
    @Mutable
    private PalettedContainer<BlockState> container;

    @SuppressWarnings("unchecked")
    @Override
    @Intrinsic
    public ChunkSection clone() {
        try {
            var self = (MixinChunkSection) super.clone();
            self.container = (PalettedContainer<BlockState>) ((CloneAccessible) self.container).clone();
            //noinspection ConstantConditions
            return (ChunkSection) (Object) self;
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }
}
