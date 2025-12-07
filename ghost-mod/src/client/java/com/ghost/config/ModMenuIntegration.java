package com.ghost.config;

import com.ghost.GhostModClient; // GhostModClientのインスタンスにアクセスするため
import com.mojang.logging.LogUtils;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ModMenuIntegration implements ModMenuApi {
    private boolean pre_connected = false;
    // UIエントリへの参照を保持（getValue()でリアルタイムな値を取得可能）
    private StringListEntry serverUriEntry;
    private IntegerListEntry serverPortEntry;
    // Mementoパターン: 画面リフレッシュ時に入力値を保持するスナップショット
    private GhostConfig.ConfigSnapshot pendingSnapshot = null;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> {
            // 新規に設定画面が開かれた時はpending値をクリア（キャンセル後の再オープン対応）
            pendingSnapshot = null;
            return createConfigScreen(parent);
        };
    }

    private Screen createConfigScreen(Screen parent) {
        // 設定データクラスのインスタンスを取得
        final GhostConfig config = GhostConfig.getInstance();

        // 初期値: pendingSnapshotがあればそれを使用、なければ保存済み設定からスナップショットを作成
        final GhostConfig.ConfigSnapshot initialSnapshot = (pendingSnapshot != null) ? pendingSnapshot
                : config.createSnapshot();

        // 1. ConfigBuilderの初期化
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.ghostmod.config"))
                .setDoesConfirmSave(false)
                .setAlwaysShowTabs(false)
                .setSavingRunnable(() -> {
                    config.save();
                    // 保存後はpending値をクリア
                    pendingSnapshot = null;
                });

        // 2. 「Network」カテゴリを作成
        final ConfigCategory networkCategory = builder
                .getOrCreateCategory(Component.translatable("category.ghostmod.network"));

        // 3. エントリー（UI要素）を作成するためのビルダーを取得
        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 4. 各設定項目をカテゴリに追加していく
        // --- サーバー設定 ---
        // エントリを作成して参照を保持（getValue()でリアルタイムな値を取得可能）
        serverUriEntry = entryBuilder.startStrField(
                Component.translatable("option.ghostmod.serverUri"), initialSnapshot.serverUri())
                .setDefaultValue("localhost")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverUri"))
                .setSaveConsumer(config::setServerUri)
                .build();
        networkCategory.addEntry(serverUriEntry);

        serverPortEntry = entryBuilder.startIntField(
                Component.translatable("option.ghostmod.serverPort"), initialSnapshot.serverPort())
                .setDefaultValue(8887)
                .setMin(1)
                .setMax(65535)
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPort"))
                .setSaveConsumer(config::setServerPort)
                .build();
        networkCategory.addEntry(serverPortEntry);

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
                    connectPushButton(connected, parent);
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

    // 現在のUI値からスナップショットを作成するヘルパーメソッド
    private GhostConfig.ConfigSnapshot createCurrentSnapshot() {
        return new GhostConfig.ConfigSnapshot(
                serverUriEntry.getValue(),
                serverPortEntry.getValue());
    }

    private void connectPushButton(boolean connected, Screen parent) {
        if (!Objects.equals(connected, this.pre_connected)) {
            LogUtils.getLogger().info("toggle state connected: {}", connected);
            if (connected) {
                // 現在のUI値からスナップショットを作成
                GhostConfig.ConfigSnapshot currentSnapshot = createCurrentSnapshot();
                try {
                    URI uri = new URI(currentSnapshot.getFullWebSocketUri());
                    var isConnected = GhostModClient.GHOST_SYNC_SERVICE.connectBlocking(uri, 3, TimeUnit.SECONDS);
                    if (isConnected) {
                        // 接続成功時: スナップショットを設定に反映して保存
                        GhostConfig config = GhostConfig.getInstance();
                        config.restoreFrom(currentSnapshot);
                        config.save();
                        pendingSnapshot = null;
                        LogUtils.getLogger().info("Successfully connected to server. Settings saved.");
                    } else {
                        // 接続失敗時: 入力値を保持して画面リフレッシュ後も維持
                        pendingSnapshot = currentSnapshot;
                        LogUtils.getLogger().error("Failed to connect server. Settings not saved.");
                    }
                } catch (Exception ex) {
                    // エラー時も入力値を保持
                    pendingSnapshot = currentSnapshot;
                    LogUtils.getLogger().error("Connection error:", ex);
                }
            } else {
                // 切断時も現在の入力値を保持
                pendingSnapshot = createCurrentSnapshot();
                GhostModClient.GHOST_SYNC_SERVICE.disconnect();
            }
            Minecraft.getInstance().setScreen(createConfigScreen(parent));
        }
        this.pre_connected = connected;
    }
}