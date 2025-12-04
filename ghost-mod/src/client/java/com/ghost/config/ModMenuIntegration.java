package com.ghost.config;

import com.ghost.GhostModClient; // GhostModClientのインスタンスにアクセスするため
import com.mojang.logging.LogUtils;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ModMenuIntegration implements ModMenuApi {
    private boolean pre_connected = false;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen; // メソッド参照で createConfigScreen メソッドを渡す
    }

    private Screen createConfigScreen(Screen parent) {
        // 設定データクラスのインスタンスを取得
        final GhostConfig config = GhostConfig.getInstance();

        // 1. ConfigBuilderの初期化
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.ghostmod.config"))
                .setDoesConfirmSave(false)
                .setAlwaysShowTabs(false)
                .setSavingRunnable(config::save);

        // 2. 「Network」カテゴリを作成
        final ConfigCategory networkCategory = builder
                .getOrCreateCategory(Component.translatable("category.ghostmod.network"));

        // 3. エントリー（UI要素）を作成するためのビルダーを取得
        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 4. 各設定項目をカテゴリに追加していく
        // --- サーバー設定 ---
        networkCategory.addEntry(entryBuilder.startStrField(
                Component.translatable("option.ghostmod.serverUri"), config.getServerUri())
                .setDefaultValue("ws://localhost")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverUri"))
                .setSaveConsumer(config::setServerUri) // 保存時に config.setServerUri() を呼ぶ
                .build());

        networkCategory.addEntry(entryBuilder.startIntField(
                Component.translatable("option.ghostmod.serverPort"), config.getServerPort())
                .setDefaultValue(8887)
                .setMin(1)
                .setMax(65535)
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPort"))
                .setSaveConsumer(config::setServerPort)
                .build());

        // --- 接続管理 ---
        // 接続状態インジケーター（説明テキストとして表示）
        networkCategory.addEntry(entryBuilder.startTextDescription(
                // ★ GhostModClientのインスタンス経由でServiceの状態を取得
                Component.translatable("option.ghostmod.status", getStatusText()))
                .build());

        // 再接続ボタン
        var reconnectButton = entryBuilder.startBooleanToggle(
                Component.translatable("option.ghostmod.connection"), GhostModClient.GHOST_SYNC_SERVICE.isConnected())
                .setYesNoTextSupplier((connected) -> {
                    connectPushButton(connected, config, parent);
                    return Component.translatable(
                            connected ? "option.ghostmod.connection.disconnect" : "option.ghostmod.connection.connect");
                })
                .build();
        reconnectButton.setEditable(Minecraft.getInstance().hasSingleplayerServer());
        networkCategory.addEntry(reconnectButton);

        // 5. ConfigBuilderからScreenをビルドして返す
        return builder.build();
    }

    // 接続状態を示すテキストを返すヘルパーメソッド
    private Component getStatusText() {
        boolean isConnected = GhostModClient.GHOST_SYNC_SERVICE.isConnected();
        return Component
                .translatable(isConnected ? "text.ghostmod.status.connected" : "text.ghostmod.status.disconnected")
                .withStyle(style -> style.withColor(isConnected ? 0x55FF55 : 0xFF5555) // 緑 or 赤
                );
    }

    private void connectPushButton(boolean connected, GhostConfig config, Screen parent) {
        if (!Objects.equals(connected, this.pre_connected)) {
            LogUtils.getLogger().info("toggle state connected: {}", connected);
            if (connected) {
                try {
                    URI uri = new URI(config.getFullWebSocketUri());
                    var isConnected = GhostModClient.GHOST_SYNC_SERVICE.connectBlocking(uri, 3, TimeUnit.SECONDS);
                    if (isConnected) {
                        LogUtils.getLogger().info("Successfully to connect server");
                    } else {
                        LogUtils.getLogger().error("Failed to connect server");
                    }
                } catch (Exception ex) {
                    LogUtils.getLogger().error("Failed to saving config:", ex);
                }
            } else {
                GhostModClient.GHOST_SYNC_SERVICE.disconnect();
            }
            Minecraft.getInstance().setScreen(createConfigScreen(parent));
        }
        this.pre_connected = connected;
    }
}