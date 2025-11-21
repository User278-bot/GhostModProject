package com.ghost.init;

import com.ghost.client.PlayerDataSender;
import com.ghost.config.GhostConfig;
import com.ghost.entity.GhostEntitySynchronizer;
import com.ghost.net.GhostSyncService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ClientEventHandler {
    private final PlayerDataSender playerDataSender;
    private final GhostEntitySynchronizer ghostEntitySynchronizer;
    private final GhostSyncService ghostSyncService;

    public ClientEventHandler(final GhostSyncService ghostSyncService,
            final GhostEntitySynchronizer ghostEntitySynchronizer) {
        this.ghostEntitySynchronizer = ghostEntitySynchronizer;
        this.ghostSyncService = ghostSyncService;
        this.playerDataSender = new PlayerDataSender(this.ghostSyncService);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventHandler.class);

    public void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isLocalServer()) {
                LOGGER.info("Joined a single player world. Auto-connecting to GhostServer...");
                try {
                    GhostConfig config = GhostConfig.getInstance();
                    URI serverUri = new URI(config.getFullWebSocketUri());
                    ghostSyncService.connect(serverUri);
                } catch (Exception ex) {
                    LOGGER.error("Failed to auto-connect to GhostServer.", ex);
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ghostSyncService.disconnect();
        });
        ClientTickEvents.END_WORLD_TICK.register((world) -> {
            playerDataSender.sendPlayerData(world);
            ghostEntitySynchronizer.onTick(world);
        });
    }
}
