package gay.ampflower.plymouth.locking.handler;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public interface IAdvancedPermissionHandler extends IPermissionHandler {
    void addPlayersByUuid(Collection<UUID> uuids, byte permissions);

    void removePlayersByUuid(Collection<UUID> uuids);

    void addPlayers(Collection<? extends PlayerEntity> players, byte permissions);

    void removePlayers(Collection<? extends PlayerEntity> players);

    Collection<UUID> getPlayersByUuid();

    void addGroups(Collection<String> groups, byte permissions);

    void removeGroups(Collection<String> groups);

    Collection<String> getGroups();

}
