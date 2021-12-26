package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.CloneAccessible;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
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
    private PalettedContainer<BlockState> blockStateContainer;

    @Shadow
    @Final
    @Mutable
    private PalettedContainer<Biome> biomeContainer;

    @Override
    @Intrinsic
    public ChunkSection clone() {
        try {
            var self = (MixinChunkSection) super.clone();
            self.blockStateContainer = self.blockStateContainer.copy();
            self.biomeContainer = self.biomeContainer.copy();
            //noinspection ConstantConditions
            return (ChunkSection) (Object) self;
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }
}
