package net.kjp12.plymouth.locking.handler;// Created 2021-03-23T13:06:08

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

    void modifyPlayers(Collection<? extends PlayerEntity> players, short permissions);

    void removePlayers(Collection<? extends PlayerEntity> players);

    Collection<UUID> getPlayersByUuid();

    void addGroups(Collection<String> groups, byte permissions);

    void removeGroups(Collection<String> groups);

    Collection<String> getGroups();

}
