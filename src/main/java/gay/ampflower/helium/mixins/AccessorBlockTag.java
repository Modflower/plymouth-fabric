package gay.ampflower.helium.mixins;

import net.minecraft.block.Block;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.GlobalTagAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockTags.class)
public interface AccessorBlockTag {
    @Accessor("ACCESSOR")
    static GlobalTagAccessor<Block> getAccessor() {
        throw new AssertionError("This can't occur normally!");
    }
}
