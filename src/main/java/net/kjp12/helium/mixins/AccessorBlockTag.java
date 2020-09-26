package net.kjp12.helium.mixins;

import net.minecraft.block.Block;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.RequiredTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockTags.class)
public interface AccessorBlockTag {
    @Accessor("REQUIRED_TAGS")
    static RequiredTagList<Block> getRequiredTags() {
        throw new AssertionError("This can't occur normally!");
    }
}
