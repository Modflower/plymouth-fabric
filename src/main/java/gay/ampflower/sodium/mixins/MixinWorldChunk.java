package gay.ampflower.sodium.mixins;

import gay.ampflower.sodium.helpers.IShadowChunk;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements Chunk, IShadowChunk {
    @Shadow
    @Final
    private ChunkSection[] sections;
    private transient ChunkSection[] sodium$shadowChunkSections;
    private transient ChunkNibbleArray[] sodium$shadowSkyLight, sodium$shadowBlockLight;

    public void sodium$generateShadow() {
        if (sodium$shadowChunkSections == null) {
            sodium$shadowChunkSections = new ChunkSection[sections.length];
        }
    }
}
