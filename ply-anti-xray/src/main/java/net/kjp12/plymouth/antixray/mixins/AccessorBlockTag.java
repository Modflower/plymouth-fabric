package net.kjp12.plymouth.antixray.mixins;

import net.minecraft.block.Block;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.RequiredTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Deprecated
@Mixin(BlockTags.class)
public interface AccessorBlockTag {
    @Accessor("REQUIRED_TAGS")
    static RequiredTagList<Block> getRequiredTags() {
        throw new AssertionError("This can't occur normally!");
    }
}
