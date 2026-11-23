package com.ghost.init;

import com.ghost.client.PlayerDataSender;
import com.ghost.client.ToastNotifications;
import com.ghost.config.GhostConfig;
import com.ghost.entity.GhostEntitySynchronizer;
import com.ghost.net.GhostSyncService;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;

//? if <= 1.20.6 {
 import net.minecraft.client.gui.screens.SkinCustomizationScreen;
//?} else {
/*import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
*///?}
import java.net.URI;
import java.util.concurrent.TimeUnit;

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
                    ghostSyncService.connectAsync(serverUri, config.getServerPassword(), 3, TimeUnit.SECONDS)
                            .thenAccept((isConnected) -> {
                                // 接続成功をスケジュールして確認（非同期接続のため）
                                if (isConnected) {
                                    client.execute(ToastNotifications::showConnectionSuccessToast);
                                }
                            }).exceptionally((e) -> {
                                LogUtils.getLogger().error("Failed to connect to GhostServer asynchronously", e);
                                return null;
                            });

                } catch (Exception ex) {
                    LogUtils.getLogger().error("Failed to auto-connect to GhostServer.", ex);
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ghostSyncService.disconnect();
            playerDataSender.reset();
        });
        //?if >= 26.1{
        /*// ゴースト状態の反映は tickEntities() の先頭で行い、1 tick の固定遅延を防ぐ
        ClientTickEvents.START_LEVEL_TICK.register(ghostEntitySynchronizer::onTick);
        // 自プレイヤーのデータ送信は当 tick の移動完了後に行う
        ClientTickEvents.END_LEVEL_TICK.register(playerDataSender::sendPlayerData);
        *///?} else {
        // ゴースト状態の反映は tickEntities() の先頭で行い、1 tick の固定遅延を防ぐ
        ClientTickEvents.START_WORLD_TICK.register(ghostEntitySynchronizer::onTick);
        // 自プレイヤーのデータ送信は当 tick の移動完了後に行う
        ClientTickEvents.END_WORLD_TICK.register(playerDataSender::sendPlayerData);
        //?}

        // スキンカスタマイズ画面が閉じられた時に即座にデータ送信
        ScreenEvents.AFTER_INIT.register(
                (client, screen, scaledWidth, scaledHeight) -> ScreenEvents.remove(screen).register((removedScreen) -> {
                    if (removedScreen instanceof SkinCustomizationScreen) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.level != null) {
                            // 画面が閉じられた時点で即座にプレイヤーデータを送信
                            playerDataSender.sendPlayerData(mc.level);
                        }
                    }
                }));
    }
}
