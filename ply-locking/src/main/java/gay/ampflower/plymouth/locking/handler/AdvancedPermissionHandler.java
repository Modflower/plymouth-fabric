package gay.ampflower.plymouth.locking.handler;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMaps;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public class AdvancedPermissionHandler extends BasicPermissionHandler implements IAdvancedPermissionHandler {
    private final Object2ByteMap<UUID> playerAccess = new Object2ByteOpenHashMap<>();
    private final Long2ByteMap groupAccess = new Long2ByteOpenHashMap();

    public AdvancedPermissionHandler() {
    }

    public AdvancedPermissionHandler(IPermissionHandler permissionHandler) {
        super(permissionHandler);
        // TODO: Player access
    }

    public AdvancedPermissionHandler(@NotNull UUID owner) {
        super(owner);
    }

    public AdvancedPermissionHandler(@NotNull UUID owner, String group) {
        super(owner, group);
    }

    @Override
    public void addPlayersByUuid(Collection<UUID> uuids, byte permissions) {
        for (var uuid : uuids) {
            playerAccess.put(uuid, (byte) (permissions & 0xF));
        }
    }

    @Override
    public void removePlayersByUuid(Collection<UUID> uuids) {
        for (var uuid : uuids) {
            playerAccess.removeByte(uuid);
        }
    }

    @Override
    public void addPlayers(Collection<? extends PlayerEntity> players, byte permissions) {
        for (var player : players) {
            playerAccess.put(player.getUuid(), permissions);
        }
    }

    @Override
    public void removePlayers(Collection<? extends PlayerEntity> players) {
        for (var player : players) {
            playerAccess.removeByte(player.getUuid());
        }
    }

    @Override
    public Collection<UUID> getPlayersByUuid() {
        return playerAccess.keySet();
    }

    @Override
    public void addGroups(Collection<String> groups, byte permissions) {
        throw new UnsupportedOperationException("Groups not implemented.");
        // for(var group : groups) {
        //     groupAccess.put(group, (byte)(permissions & 0xF));
        // }
    }

    @Override
    public void removeGroups(Collection<String> groups) {
        throw new UnsupportedOperationException("Groups not implemented.");
    }

    @Override
    public Collection<String> getGroups() {
        throw new UnsupportedOperationException("Groups not implemented.");
    }

    @Override
    public boolean allowRead(UUID uuid) {
        return (playerAccess.getByte(uuid) & 8) != 0 || super.allowRead(uuid);
    }

    @Override
    public boolean allowWrite(UUID uuid) {
        return (playerAccess.getByte(uuid) & 4) != 0 || super.allowWrite(uuid);
    }

    @Override
    public boolean allowDelete(UUID uuid) {
        return (playerAccess.getByte(uuid) & 2) != 0 || super.allowDelete(uuid);
    }

    @Override
    public boolean allowPermissions(UUID uuid) {
        return (playerAccess.getByte(uuid) & 1) != 0 || super.allowPermissions(uuid);
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        // Legacy handling
        if (tag.contains("access", 9)) {
            for (var a : tag.getList("access", 10)) {
                var access = (CompoundTag) a;
                if (access.containsUuid("t") && access.contains("p", 99)) {
                    playerAccess.put(access.getUuid("t"), reversePermissionBits(access.getByte("p")));
                }
            }
        }
        if (tag.contains("players", 9)) {
            for (var a : tag.getList("players", 10)) {
                var access = (CompoundTag) a;
                if (access.containsUuid("u") && access.contains("p", 99)) {
                    playerAccess.put(access.getUuid("u"), access.getByte("p"));
                }
            }
        }
        if (tag.contains("groups", 9)) {
            for (var a : tag.getList("groups", 10)) {
                var access = (CompoundTag) a;
                if (access.contains("g", 99) && access.contains("p", 99)) {
                    groupAccess.put(access.getLong("g"), access.getByte("p"));
                }
            }
        }
    }

    /**
     * This transforms the permission bits to use rwdp rather than dwr.
     */
    private static byte reversePermissionBits(byte b) {
        // normalises to 0b111
        b &= 7;
        // Flip the least significant 4 bits. Taken from Integer.
        b = (byte) ((b & 0x5) << 1 | (b >>> 1) & 0x5);
        b = (byte) ((b & 0x3) << 2 | (b >>> 2) & 0x3);
        return b;
    }

    @Override
    public void toTag(CompoundTag tag) {
        super.toTag(tag);
        if (!playerAccess.isEmpty()) {
            var players = new ListTag();
            var itr = Object2ByteMaps.fastIterator(playerAccess);
            while (itr.hasNext()) {
                var e = itr.next();
                var player = new CompoundTag();
                player.putUuid("u", e.getKey());
                player.putByte("p", e.getByteValue());
                players.add(player);
            }
            tag.put("players", players);
        }
        if (!groupAccess.isEmpty()) {
            var groups = new ListTag();
            var itr = Long2ByteMaps.fastIterator(groupAccess);
            while (itr.hasNext()) {
                var e = itr.next();
                var group = new CompoundTag();
                group.putLong("g", e.getLongKey());
                group.putByte("p", e.getByteValue());
                groups.add(group);
            }
            tag.put("groups", groups);
        }
    }
}
