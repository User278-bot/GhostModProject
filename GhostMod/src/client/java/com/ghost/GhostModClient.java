package com.ghost;

import java.net.URI;
import java.util.Objects;

import com.ghost.common.dto.PlayerData;
import com.ghost.converter.PlayerDataConverter;
import com.ghost.net.ConnectionManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GhostModClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModClient.class);

    @Nullable
    private URI serverUri = null;
    private final ConnectionManager connection = new ConnectionManager();
    private static int duration = 1;      // >0
    private static final int FORCE_DURATION = 40;
    @Nullable
    private PlayerData lastSentData = null;

    public GhostModClient() {
    }

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        LOGGER.info("Initializing GhostModClient...");
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    if (client.isLocalServer()) {
                        LOGGER.info("Joined a single play world. Connecting to GhostServer...");
                        try {
                            //後にメニューから変更可能にするが、mvp環境のため直打ち
                            serverUri = URI.create("ws://localhost:8887");
                            connection.connect(serverUri);
                        } catch (Exception ex) {
                            LOGGER.error("Failed to connect GhostServer.", ex);
                        }
                    }
                }
        );
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    if (connection.isOpen()) {
                        connection.disconnect();
                    }
                }
        );
        ClientTickEvents.END_WORLD_TICK.register(
                (world) -> {
                    if (!connection.isOpen()) {
                        return;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) {
                        return;
                    }

                    final PlayerData current_data = PlayerDataConverter.fromPlayer(mc.player);
                    // 1. 状態が変化したかどうかを判定
                    final boolean isStateChanged = hasPlayerStateChanged(current_data) && world.getGameTime() % duration == 0;

                    // 2. 強制送信のタイミングかどうかを判定
                    final boolean isForceSendTime = (world.getGameTime() % FORCE_DURATION == 0);

                    if (isStateChanged || isForceSendTime) {
                        connection.sendPlayerData(current_data);
                        lastSentData = current_data;
                    }
                }
        );

    }

    private boolean hasPlayerStateChanged(PlayerData currentData) {
        // 最初に送信するときは必ず送信する
        if (lastSentData == null) {
            return true;
        }

        // 位置が0.01ブロック以上動いたか
        if (lastSentData.pos().distanceToSqr(currentData.pos()) > 0.01 * 0.01) {
            return true;
        }

        // 向きが変わったか
        if (lastSentData.rot().distanceToSqr(currentData.rot()) > 1f) {
            return true;
        }

        // ポーズが変わったか
        if (!Objects.equals(lastSentData.pose(), currentData.pose())) {
            return true;
        }

        // 変化がなければ送信しない
        return false;
    }
}