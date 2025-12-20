package com.ghost.init;

import com.ghost.client.PlayerDataSender;
import com.ghost.config.GhostConfig;
import com.ghost.entity.GhostEntitySynchronizer;
import com.ghost.net.GhostSyncService;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;

import java.net.URI;

import static com.ghost.client.ToastNotifications.showConnectionSuccessToast;

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

    public void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.isLocalServer()) {
                LogUtils.getLogger().info("Joined a single player world. Auto-connecting to GhostServer...");
                try {
                    GhostConfig config = GhostConfig.getInstance();
                    URI serverUri = new URI(config.getFullWebSocketUri());
                    ghostSyncService.connect(serverUri, config.getServerPassword());
                    // 接続成功をスケジュールして確認（非同期接続のため）
                    client.execute(() -> {
                        if (ghostSyncService.isConnected()) {
                            showConnectionSuccessToast();
                        }
                    });
                } catch (Exception ex) {
                    LogUtils.getLogger().error("Failed to auto-connect to GhostServer.", ex);
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ghostSyncService.disconnect());
        ClientTickEvents.END_WORLD_TICK.register((world) -> {
            playerDataSender.sendPlayerData(world);
            ghostEntitySynchronizer.onTick(world);
        });

        // スキンカスタマイズ画面が閉じられた時に即座にデータ送信
        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> ScreenEvents.remove(screen).register((removedScreen) -> {
                    // スキンカスタマイズ画面かどうかをチェック
                    if (removedScreen.getClass().getSimpleName().equals(SkinCustomizationScreen.class.toString())) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.level != null) {
                            // 画面が閉じられた時点で即座にプレイヤーデータを送信
                            playerDataSender.sendPlayerData(mc.level);
                        }
                    }
                }));
    }
}
