package gay.ampflower.plymouth.locking.mixins.entities;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.Locking;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EnderDragonEntity.class)
public abstract class MixinEnderDragon extends MobEntity {
    private MixinEnderDragon(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * @author Ampflower
     * @reason Cancelling removeBlock when locked container, and moving the griefing check to before the entire loop.
     */
    @Overwrite
    private boolean destroyBlocks(Box box) {
        int x1 = MathHelper.floor(box.minX), y1 = MathHelper.floor(box.minY), z1 = MathHelper.floor(box.minZ),
                x2 = MathHelper.floor(box.maxX), y2 = MathHelper.floor(box.maxY), z2 = MathHelper.floor(box.maxZ);
        // Moved grief to not call it a hundred times, it only needs to be called once.
        boolean grief = world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING), b1 = false, b2 = false;
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) {
                    var pos = new BlockPos(x, y, z);
                    var state = world.getBlockState(pos);
                    if (!state.isAir() && state.getMaterial() != Material.FIRE) {
                        if (grief && !state.isIn(BlockTags.DRAGON_IMMUNE) && (!state.hasBlockEntity() || Locking.canBreak((ILockable) world.getBlockEntity(pos), this))) {
                            b2 = world.removeBlock(pos, false) || b2;
                        } else {
                            b1 = true;
                        }
                    }
                }
        if (b2)
            world.syncWorldEvent(2008, new BlockPos(x1 + random.nextInt(x2 - x1 + 1), y1 + random.nextInt(y2 - y1 + 1), z1 + random.nextInt(z2 - z1 + 1)), 0);
        return b1;
    }
}
