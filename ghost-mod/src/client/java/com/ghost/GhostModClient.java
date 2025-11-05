package com.ghost;

import com.ghost.common.registry.IGhostRegistry;
import com.ghost.init.ClientEventHandler;
import com.ghost.init.EntityRegistration;
import com.ghost.net.ConnectionManager;
import com.ghost.registry.InMemoryGhostRegistry;
import com.ghost.renderer.GhostRenderer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GhostModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModClient.class);
    private static final IGhostRegistry GHOST_REGISTRY = new InMemoryGhostRegistry();
    private final ConnectionManager connection;
    private final GhostRenderer ghostRenderer;
    private final PlayerDataSender playerDataSender;
    private final ClientEventHandler clientEventHandler;

    public GhostModClient() {
        connection = new ConnectionManager(GHOST_REGISTRY);
        ghostRenderer = new GhostRenderer(connection.getGhostRegistry());
        playerDataSender = new PlayerDataSender(connection);
        clientEventHandler = new ClientEventHandler(connection, playerDataSender, ghostRenderer);
    }

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        LOGGER.info("Initializing GhostModClient...");

        clientEventHandler.registerEvents();
        EntityRegistration.register();
    }
}