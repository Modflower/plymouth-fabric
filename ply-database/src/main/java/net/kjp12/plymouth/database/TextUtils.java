package net.kjp12.plymouth.database;// Created 2021-01-06T16:33:45

import net.kjp12.plymouth.common.UUIDHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.property.Property;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * @author KJP12
 * @since ${version}
 **/
public final class TextUtils {
    public static final Style
            unknown = Style.EMPTY.withColor(Formatting.RED),
            atBlock = Style.EMPTY.withColor(Formatting.YELLOW),
            entity = Style.EMPTY.withColor(Formatting.BLUE);

    public static MutableText positionToText(Vec3d pos) {
        return new TranslatableText("chat.coordinates", pos.getX(), pos.getY(), pos.getZ());
    }

    public static MutableText positionToText(Vec3i pos) {
        return new TranslatableText("chat.coordinates", pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Formats the input time to ISO Local Date with a hover card showing the full date in RFC 1123 format.
     *
     * @param instant The time to format.
     * @return The formatted time as a literal text.
     */
    public static Text timeToText(Instant instant) {
        var time = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        return new LiteralText(DateTimeFormatter.ISO_LOCAL_DATE.format(time)).styled(s -> s.withFormatting(Formatting.GRAY).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(DateTimeFormatter.RFC_1123_DATE_TIME.format(time)))));
    }

    private static <T extends Comparable<T>> Text mkPropertyText(BlockState state, Property<T> property) {
        return new LiteralText(property.getName()).formatted(Formatting.BLUE).append(": ").append(new LiteralText(property.name(state.get(property))).formatted(Formatting.GRAY)).append("\n");
    }

    /**
     * Appends the properties of the block onto the input mutable text.
     *
     * @param state The block state to fetch the properties of.
     * @param input The mutable text to append properties to.
     * @return input mutated with all the properties appended.
     */
    private static MutableText mkBlockStateHoverCard(BlockState state, MutableText input) {
        var props = state.getProperties();
        if (!props.isEmpty()) {
            input.append("\n");
            for (var prop : props) {
                input.append(mkPropertyText(state, prop));
            }
        }
        return input;
    }

    /**
     * Returns a text derived from the given block, including a hover card of the properties mimicking the in-inventory
     * hover card.
     *
     * @param state The block state to format into a text object.
     * @return The translatable text based off of the input.
     */
    public static MutableText blockToText(BlockState state) {
        var block = state.getBlock();
        var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, mkBlockStateHoverCard(state, new TranslatableText(block.getTranslationKey())).append("\n").append(new LiteralText(Registry.BLOCK.getId(block).toString()).formatted(Formatting.DARK_GRAY)));
        return new TranslatableText(block.getTranslationKey()).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.LIGHT_PURPLE));
    }

    /**
     * Returns a text derived from the given stack, including a hover card of the item.
     *
     * @param stack The item stack to format into a text object.
     * @return The literal text based off of the input.
     */
    public static MutableText itemToText(ItemStack stack) {
        var text = new LiteralText("").append(stack.getName());
        var style = text.getStyle();
        if (stack.hasCustomName()) style = style.withFormatting(Formatting.ITALIC);
        var fmt = stack.getRarity().formatting;
        style = style.withFormatting(fmt == Formatting.WHITE ? Formatting.LIGHT_PURPLE : fmt);
        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack)));
        return text.setStyle(style);
    }

    public static MutableText lookupPlayerToText(@Nullable MinecraftServer server, UUID uuid) {
        if ((uuid.getMostSignificantBits() & UUIDHelper.LOW_BITS) == UUIDHelper.PLYMOUTH_HEADER) {
            // We're forced to do a lookup.
            return playerToText(null, null, Objects.requireNonNullElse(DatabaseHelper.database.getPlayerName(uuid), "plymouth:unknown"), uuid, null);
        } else {
            if (server != null) {
                var player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) return player.getName().copy();
                var profile = server.getUserCache().getByUuid(uuid);
                if (profile != null) return playerToText(profile.getName(), uuid);
            }
            var name = DatabaseHelper.database.getPlayerName(uuid); // This ideally shouldn't occur normally.
            return playerToText(Objects.requireNonNullElse(name, "Unknown Player"), uuid);
        }
    }

    public static MutableText playerToText(String userName, UUID userId, @Nullable UUID entityId) {
        return playerToText(null, null, userName, userId, entityId);
    }

    /**
     * Returns a text derived from the given username, user UUID and entity UUID.
     *
     * @param world    The world to name if the user ID is a world.
     * @param pos      The position to name if the user ID is a block.
     * @param userName The username of the user. Can also be an identifier.
     * @param userId   The UUID of the user. Maybe an NPC-based Plymouth identifier of an entity, block or world.
     * @param entityId The raw UUID of the entity should it not be a player.
     * @return Either the literal or translatable text based off of the input.
     */
    public static MutableText playerToText(World world, BlockPos pos, String userName, UUID userId, @Nullable UUID entityId) {
        if (UUIDHelper.isBlock(userId)) {
            return blockToText(pos, userName);
        } else if (UUIDHelper.isEntity(userId)) {
            return entityToText(userName, userId, entityId);
        } else if (UUIDHelper.isDamageSource(userId) || UUIDHelper.isWorld(userId) /*The world is an undefined format currently. For now, it shall fall through. TODO: implement world schema*/) {
            // We really have nothing we can do to lookup here,
            // nor should we really need to as the damage source is saved as helium:<src>.
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(userName + '\n' + userId));
            var i = userName.indexOf(':');
            return new LiteralText(i == -1 ? userName : userName.substring(i + 1)).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.DARK_AQUA));
        } else {
            return playerToText(userName, userId);
        }
    }

    public static MutableText blockToText(BlockPos pos, String name) {
        if (name == null) {
            return positionToText(pos).setStyle(unknown);
        }
        // Do a block lookup using userName as an identifier.
        var optional = Registry.BLOCK.getOrEmpty(Identifier.tryParse(name));
        if (optional.isPresent()) {
            // We have a valid block, we can make use of the translation key.
            var key = optional.get().getTranslationKey();
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText(key).append("\n").append(new LiteralText(name).formatted(Formatting.DARK_GRAY)));
            return (pos != null ? positionToText(pos) : new TranslatableText(key)).setStyle(atBlock.withHoverEvent(onHover));
        } else {
            // We don't have a valid block. Perhaps it was removed? Use the non-translatable version of the return to make a reasonable string.
            var i = name.indexOf(':');
            var str = i + 1 < name.length() ? Character.toUpperCase(name.charAt(i + 1)) + name.substring(i + 2) : name;
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(str + '\n').append(new LiteralText(name).formatted(Formatting.DARK_GRAY)));
            return (pos != null ? positionToText(pos) : new LiteralText(str)).setStyle(atBlock.withHoverEvent(onHover));
        }
    }

    public static MutableText entityToText(String userName, UUID userId, UUID entityId) {
        // Do an entity lookup using userName as an identifier.
        var optional = EntityType.get(userName);
        if (optional.isPresent()) {
            // We have a valid entity, make it based off of the SHOW_ENTITY hover event action.
            var type = optional.get();
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(type, Objects.requireNonNullElse(entityId, userId), new TranslatableText(type.getTranslationKey())));
            return new TranslatableText(type.getTranslationKey()).setStyle(entity.withHoverEvent(onHover));
        } else {
            // We don't have a valid entity, we'll have to resort to manually generating the string.
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(userName + '\n' + Objects.requireNonNullElse(entityId, userId)));
            var i = userName.indexOf(':');
            return new LiteralText(i == -1 ? userName : userName.substring(i + 1)).setStyle(entity.withHoverEvent(onHover));
        }
    }

    public static MutableText playerToText(String name, UUID uuid) {
        // We're a player, we'll just present the output as one. Note: This may produce screwy output with legacy database schemas.
        var onHover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(EntityType.PLAYER, uuid, new LiteralText(name /*no name lookup required*/)));
        return new LiteralText(name).styled(s -> s.withHoverEvent(onHover));
    }
}
