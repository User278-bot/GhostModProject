package com.ghost.client;

import com.ghost.api.dto.PlayerData;
import com.ghost.converter.PlayerDataConverter;
import com.ghost.net.GhostSyncService;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public class PlayerDataSender {
    private static final int FORCE_SEND_INTERVAL = 40;

    @Nullable
    private PlayerData lastSentData = null;
    private final GhostSyncService ghostSyncService;

    public PlayerDataSender(GhostSyncService ghostSyncService) {
        this.ghostSyncService = ghostSyncService;
    }

    /**
     * 前回送信済みデータのキャッシュをリセットします。
     * 切断・再接続時に初回状態を確実に再送するために使用します。
     */
    public void reset() {
        lastSentData = null;
    }

    public void sendPlayerData(ClientLevel world) {
        // 未接続時は毎 tick の DTO 変換・装備シリアライズ処理自体をスキップする
        if (!ghostSyncService.isConnected()) {
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
            final var packet = new GhostPacket<>(MessageType.UPDATE, currentData);
            ghostSyncService.sendPacket(packet);
            lastSentData = currentData;
        }
    }

    private boolean hasPlayerStateChanged(PlayerData currentData) {
        // 最初に送信するときは必ず送信する
        if (lastSentData == null) {
            return true;
        }
        return !lastSentData.equals(currentData);
    }
}
