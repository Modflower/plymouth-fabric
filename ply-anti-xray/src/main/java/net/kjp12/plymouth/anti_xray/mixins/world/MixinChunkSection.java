package net.kjp12.plymouth.anti_xray.mixins.world;// Created 2021-03-30T18:30:23

import net.kjp12.plymouth.anti_xray.CloneAccessible;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.*;

/**
 * Forces the chunk section to be cloneable.
 *
 * @author KJP12
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
