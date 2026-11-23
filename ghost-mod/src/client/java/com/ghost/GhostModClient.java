package com.ghost;

import com.ghost.api.registry.IGhostRegistry;
import com.ghost.init.ClientEventHandler;
import com.ghost.init.EntityRegistration;
import com.ghost.init.GhostModKeyBindings;
import com.ghost.net.GhostSyncService;
import com.ghost.registry.InMemoryGhostRegistry;
import com.ghost.entity.GhostEntitySynchronizer;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;

public class GhostModClient implements ClientModInitializer {
    private static final IGhostRegistry GHOST_REGISTRY = new InMemoryGhostRegistry();
    private final ClientEventHandler clientEventHandler;

    public static final GhostSyncService GHOST_SYNC_SERVICE = new GhostSyncService(GHOST_REGISTRY);

    public GhostModClient() {
        GhostEntitySynchronizer ghostEntitySynchronizer = new GhostEntitySynchronizer(GHOST_REGISTRY);
        clientEventHandler = new ClientEventHandler(GHOST_SYNC_SERVICE, ghostEntitySynchronizer);
    }

    @Override
    public void onInitializeClient() {
        LogUtils.getLogger().debug("Initializing GhostModClient...");

        clientEventHandler.registerEvents();
        EntityRegistration.register();
        GhostModKeyBindings.register();
    }
}