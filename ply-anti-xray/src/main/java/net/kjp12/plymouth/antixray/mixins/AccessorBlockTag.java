package net.kjp12.plymouth.antixray.mixins;

import net.minecraft.block.Block;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Deprecated
@Mixin(BlockTags.class)
public interface AccessorBlockTag {
    @Invoker
    static TagKey<Block> invokeRegister(String id) {
        throw new AssertionError("This can't occur normally!");
    }
}
