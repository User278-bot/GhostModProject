package com.ghost.init;

import com.ghost.PlayerDataSender;
import com.ghost.common.registry.IGhostRegistry;
import com.ghost.renderer.GhostRenderer;
import com.ghost.net.GhostSyncService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventHandler {
    private final PlayerDataSender playerDataSender;
    private final GhostRenderer ghostRenderer;
    private final GhostSyncService ghostSyncService;

    public ClientEventHandler(final IGhostRegistry ghostRegistry, final GhostSyncService ghostSyncService, final GhostRenderer ghostRenderer) {
        this.ghostRenderer = ghostRenderer;
        this.ghostSyncService = ghostSyncService;
        this.playerDataSender = new PlayerDataSender(this.ghostSyncService);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventHandler.class);

    public void registerEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ghostSyncService.disconnect();
        });
        ClientTickEvents.END_WORLD_TICK.register((world) -> {
            playerDataSender.sendPlayerData(world);
            ghostRenderer.onTick(world);
        });
    }
}
