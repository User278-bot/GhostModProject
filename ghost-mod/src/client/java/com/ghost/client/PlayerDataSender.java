package com.ghost.client;

import com.ghost.api.dto.PlayerData;
import com.ghost.converter.PlayerDataConverter;
import com.ghost.net.GhostSyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public class PlayerDataSender {
    private static final int FORCE_SEND_INTERVAL = 40;

    @Nullable
    private volatile PlayerData lastSentData = null;
    private final GhostSyncService ghostSyncService;

    public PlayerDataSender(GhostSyncService ghostSyncService) {
        this.ghostSyncService = ghostSyncService;
    }

    /**
     * 前回送信状態をリセットし、次回送信時に必ず最新状態を送出するようにします。
     */
    public void reset() {
        this.lastSentData = null;
    }

    public void sendPlayerData(ClientLevel world) {
        if (!ghostSyncService.isConnected()) {
            this.lastSentData = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        final PlayerData currentData = PlayerDataConverter.fromPlayer(mc.player);
        final boolean isStateChanged = hasPlayerStateChanged(currentData);
        final boolean isForceSendTime = (world.getGameTime() % FORCE_SEND_INTERVAL == 0);

        if (isStateChanged || isForceSendTime) {
            ghostSyncService.sendPlayerUpdate(currentData);
            this.lastSentData = currentData;
        }
    }

    private boolean hasPlayerStateChanged(PlayerData currentData) {
        PlayerData previous = this.lastSentData;
        return previous == null || !previous.equals(currentData);
    }
}
