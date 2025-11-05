package com.ghost;

import com.ghost.common.dto.PlayerData;
import com.ghost.converter.PlayerDataConverter;
import com.ghost.net.ConnectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayerDataSender {
    @Nullable
    private PlayerData lastSentData = null;
    private final ConnectionManager connectionManager;
    private static final int duration = 1;      // >0
    private static final int FORCE_DURATION = 40;

    public PlayerDataSender(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void sendPlayerData(ClientLevel world) {
        if (!connectionManager.isOpen()) {
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
            connectionManager.sendPlayerData(current_data);
            lastSentData = current_data;
        }
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
