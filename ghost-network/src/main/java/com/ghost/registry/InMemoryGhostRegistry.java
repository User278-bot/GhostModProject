package com.ghost.registry;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.registry.IGhostRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGhostRegistry implements IGhostRegistry {
    private final Map<String, PlayerData> ghosts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdates = new ConcurrentHashMap<>();

    @Override
    public void updateGhost(PlayerData data) {
        if (data != null && !data.uuid().isEmpty()) {
            ghosts.put(data.uuid(), data);
            lastUpdates.put(data.uuid(), System.currentTimeMillis());
        }
    }

    @Override
    public Optional<PlayerData> removeGhost(String uuid) {
        lastUpdates.remove(uuid);
        return Optional.ofNullable(ghosts.remove(uuid));
    }

    @Override
    public void clear() {
        ghosts.clear();
        lastUpdates.clear();
    }

    @Override
    public Optional<PlayerData> getGhost(String uuid) {
        return Optional.ofNullable(ghosts.get(uuid));
    }

    @Override
    public Collection<PlayerData> getAllGhosts() {
        return List.copyOf(ghosts.values());
    }

    @Override
    public void cleanupGhosts(long timeoutMillis) {
        long limit = System.currentTimeMillis() - timeoutMillis;
        lastUpdates.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue() < limit;
            if (expired) {
                ghosts.remove(entry.getKey());
            }
            return expired;
        });
    }

    @Override
    public int getGhostCount() {
        return ghosts.size();
    }
}
