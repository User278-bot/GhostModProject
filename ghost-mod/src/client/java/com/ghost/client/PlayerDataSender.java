package com.ghost.client;

import com.ghost.api.dto.PlayerData;
import com.ghost.converter.PlayerDataConverter;
import com.ghost.net.GhostSyncService;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.packet.MessageType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayerDataSender {
    private static final int SEND_INTERVAL = 1;
    private static final int FORCE_SEND_INTERVAL = 40;
    private static final double POSITION_THRESHOLD_SQR = 0.01 * 0.01;
    private static final float ROTATION_THRESHOLD_SQR = 1.0f;

    @Nullable
    private PlayerData lastSentData = null;
    private final GhostSyncService ghostSyncService;

    public PlayerDataSender(GhostSyncService ghostSyncService) {
        this.ghostSyncService = ghostSyncService;
    }

    public void sendPlayerData(ClientLevel world) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        final PlayerData current_data = PlayerDataConverter.fromPlayer(mc.player);
        // 1. 状態が変化したかどうかを判定
        final boolean isStateChanged = hasPlayerStateChanged(current_data) && world.getGameTime() % SEND_INTERVAL == 0;

        // 2. 強制送信のタイミングかどうかを判定
        final boolean isForceSendTime = (world.getGameTime() % FORCE_SEND_INTERVAL == 0);

        if (isStateChanged || isForceSendTime) {
            final var packet = new GhostPacket<>(MessageType.UPDATE, current_data);
            ghostSyncService.sendPacket(packet);
            lastSentData = current_data;
        }
    }

    private boolean hasPlayerStateChanged(PlayerData currentData) {
        // 最初に送信するときは必ず送信する
        if (lastSentData == null) {
            return true;
        }

        // 位置が一定以上動いたか
        if (lastSentData.pos().distanceToSqr(currentData.pos()) > POSITION_THRESHOLD_SQR) {
            return true;
        }

        // 向きが変わったか
        if (lastSentData.rot().distanceToSqr(currentData.rot()) > ROTATION_THRESHOLD_SQR) {
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
