package com.ghost.api.registry; // or com.ghost.repository

import com.ghost.api.dto.PlayerData;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages the state of all ghost players in the client.
 * This class is the single source of truth for ghost data.
 */
public interface IGhostRegistry { // ★インターフェースとして定義するのが良いプラクティス

    /**
     * Adds a new ghost or updates the state of an existing ghost.
     * 
     * @param data The latest PlayerData received from the network.
     */
    void updateGhost(PlayerData data);

    /**
     * Removes a ghost from the registry, typically when a player disconnects.
     * 
     * @param uuid The UUID of the player to remove.
     */
    Optional<PlayerData> removeGhost(String uuid);

    /**
     * Removes all ghosts from the registry.
     * Called when the player disconnects from the world.
     */
    void clear();

    /**
     * Retrieves the PlayerData for a specific ghost.
     * 
     * @param uuid The UUID of the ghost to retrieve.
     * @return An Optional containing the PlayerData if the ghost exists, otherwise
     *         an empty Optional.
     */
    Optional<PlayerData> getGhost(String uuid);

    /**
     * Retrieves a collection of all currently managed ghosts.
     * This is intended to be used by the rendering layer each frame.
     * 
     * @return A thread-safe collection of PlayerData.
     */
    Collection<PlayerData> getAllGhosts();

    /**
     * Removes ghosts that haven't been updated for the specified duration.
     * 
     * @param timeoutMillis Data expiration time in milliseconds.
     */
    void cleanupGhosts(long timeoutMillis);

    /**
     * Gets the number of ghosts currently being managed.
     * 
     * @return The number of ghosts.
     */
    int getGhostCount();
}