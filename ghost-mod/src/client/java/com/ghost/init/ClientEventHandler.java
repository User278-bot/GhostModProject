package com.ghost.init;

import com.ghost.PlayerDataSender;
import com.ghost.net.ConnectionManager;
import com.ghost.renderer.GhostRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ClientEventHandler {
    private ClientEventHandler() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventHandler.class);

    public static void registerEvents(final ConnectionManager connectionManager, final PlayerDataSender playerDataSender, final GhostRenderer ghostRenderer) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isLocalServer()) {
                LOGGER.info("Joined a single play world. Connecting to GhostServer...");
                try {
                    //後にメニューから変更可能にするが、mvp環境のため直打ち
                    final URI serverUri = URI.create("ws://localhost:8887");
                    connectionManager.connect(serverUri);
                } catch (Exception ex) {
                    LOGGER.error("Failed to connect GhostServer.", ex);
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (connectionManager.isOpen()) {
                connectionManager.disconnect();
            }
        });
        ClientTickEvents.END_WORLD_TICK.register((world) -> {
            playerDataSender.sendPlayerData(world);
            ghostRenderer.onTick(world);
        });
    }
}
