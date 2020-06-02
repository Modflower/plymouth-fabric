package net.kjp12.sodium;

import net.kjp12.sodium.helpers.IShadowBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.UUID;

/**
 * This class is a late-init-type class. It's expecting that
 * by the time that it's called, all blocks would have been
 * registered.
 */
public class BlockHelper {
    /**
     * Anonymous Header Hashes, used for generating a UUID for blocks and entities
     * to interact with locked blocks. Most will be cached here, but there will be
     * some that need to be generated on the fly, ie. new blocks or entities.
     * <p>
     * If a new block or entity does try to modify a block, it will have full access
     * due to no permission checks existing for the new code. Beware of this.
     */
    public static final int
            SODIUM_HASH = "Sodium".hashCode(),
            ENTITY_HASH = "Entity".hashCode(),
            BLOCK_HASH = "Block".hashCode();
    public static final long
            ENTITY_HEADER = ((long) SODIUM_HASH) << 32L | ((long) ENTITY_HASH),
            BLOCK_HEADER = ((long) SODIUM_HASH) << 32L | ((long) BLOCK_HASH);
    public static final UUID
            ANONYMOUS_UUID = Util.field_25140,
            HOPPER_UUID = new UUID(BLOCK_HEADER, "TheHopper".hashCode()),
            TNT_UUID = new UUID(ENTITY_HEADER, "ThePrimedTNT".hashCode()),
            CREEPER_UUID = new UUID(ENTITY_HEADER, "TheCreeper".hashCode()),
            DRAGON_UUID = new UUID(ENTITY_HEADER, "TheEnderDragon".hashCode()),
            WITHER_UUID = new UUID(ENTITY_HEADER, "TheWither".hashCode());

    public static final Tag<Block> HIDDEN_BLOCKS = BlockTags.getContainer().get(new Identifier("sodium", "hidden"));

    public static UUID getUUID(Entity breaker) {
        if (breaker == null) {
            return ANONYMOUS_UUID;
        } else if (breaker instanceof TntEntity) {
            var cause = ((TntEntity) breaker).getCausingEntity();
            return cause == null ? TNT_UUID : getUUID(cause);
        } else if (breaker instanceof CreeperEntity) {
            return CREEPER_UUID;
        } else if (breaker instanceof WitherEntity) {
            return WITHER_UUID;
        } else if (breaker instanceof EnderDragonEntity) {
            return DRAGON_UUID;
        } else return breaker.getUuid();
    }

    /**
     * Protection check blocks. If the cause is a non-player entity,
     * it'll attempt to get the cause of player's UUID or use the
     * anonymous hashes.
     *
     * @return true if the entity can break the block.
     */
    public static boolean canBreak(IShadowBlockEntity block, Entity breaker) {
        return block == null || (breaker instanceof PlayerEntity ? block.sodium$canBreakBlock((PlayerEntity) breaker) : block.sodium$canBreakBlock(getUUID(breaker)));
    }
}
