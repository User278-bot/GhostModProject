package com.ghost.config;

import com.ghost.GhostModClient;
import com.mojang.logging.LogUtils;
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

import static com.ghost.client.ToastNotifications.showConnectionSuccessToast;

/**
 * Ghost Modの設定画面を作成するクラス。
 * Cloth Config APIを使用し、ModMenuに依存しません。
 */
public class GhostConfigScreen {
    private boolean pre_connected = false;
    // UIエントリへの参照を保持
    private StringListEntry serverUriEntry;
    private IntegerListEntry serverPortEntry;
    private StringListEntry serverPasswordEntry;
    // Mementoパターン: 画面リフレッシュ時に入力値を保持するスナップショット
    private GhostConfig.ConfigSnapshot pendingSnapshot = null;

    // シングルトンインスタンス（状態を保持するため）
    private static final GhostConfigScreen INSTANCE = new GhostConfigScreen();

    /**
     * 設定画面を作成します（静的ファクトリメソッド）
     * 
     * @param parent 親画面
     * @return 設定画面
     */
    public static Screen create(Screen parent) {
        // 新規に設定画面が開かれた時はpending値をクリア（キャンセル後の再オープン対応）
        INSTANCE.pendingSnapshot = null;
        return INSTANCE.createConfigScreen(parent);
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
        // エントリは保存済み設定値で作成（Cloth Configの変更検知の基準となる）
        serverUriEntry = entryBuilder.startStrField(
                Component.translatable("option.ghostmod.serverUri"), config.getServerUri())
                .setDefaultValue("localhost")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverUri"))
                .setSaveConsumer(config::setServerUri)
                .build();
        // pendingがあれば表示値を更新（変更検知が有効になりSaveボタンが活性化）
        if (pendingSnapshot != null) {
            serverUriEntry.setValue(pendingSnapshot.serverUri());
        }
        networkCategory.addEntry(serverUriEntry);

        serverPortEntry = entryBuilder.startIntField(
                Component.translatable("option.ghostmod.serverPort"), config.getServerPort())
                .setDefaultValue(8887)
                .setMin(1)
                .setMax(65535)
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPort"))
                .setSaveConsumer(config::setServerPort)
                .build();
        // pendingがあれば表示値を更新
        if (pendingSnapshot != null) {
            serverPortEntry.setValue(String.valueOf(pendingSnapshot.serverPort()));
        }
        networkCategory.addEntry(serverPortEntry);

        // パスワード設定
        serverPasswordEntry = entryBuilder.startStrField(
                Component.translatable("option.ghostmod.serverPassword"), config.getServerPassword())
                .setDefaultValue("changeme")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPassword"))
                .setSaveConsumer(config::setServerPassword)
                .build();

        // リフレクションを使用してTextFieldListEntryの保護されたtextFieldWidgetにアクセスし、
        // パスワードをマスクするためのrenderTextProviderを設定
        try {
            java.lang.reflect.Field widgetField = me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry.class
                    .getDeclaredField("textFieldWidget");
            widgetField.setAccessible(true);
            net.minecraft.client.gui.components.EditBox widget = (net.minecraft.client.gui.components.EditBox) widgetField
                    .get(serverPasswordEntry);
            if (widget != null) {
                widget
                //? if >=1.21.11 {
                /*.addFormatter
                *///?} else {
                 .setFormatter
                //?}
                ((text, firstCharacterIndex) -> Component.literal("*".repeat(text.length()))
                        .getVisualOrderText());
            }
        } catch (Exception e) {
            // リフレクション失敗時は警告を出すがクラッシュはしない
            LogUtils.getLogger().error("Failed to set password mask", e);
        }

        // pendingがあれば表示値を更新
        if (pendingSnapshot != null) {
            serverPasswordEntry.setValue(pendingSnapshot.serverPassword());
        }
        networkCategory.addEntry(serverPasswordEntry);

        // --- 接続管理 ---
        // 接続状態インジケーター
        networkCategory.addEntry(entryBuilder.startTextDescription(
                Component.translatable("option.ghostmod.status", getStatusText()))
                .build());

        // 接続先URI表示（接続中の場合のみ）
        URI connectedUri = GhostModClient.GHOST_SYNC_SERVICE.getConnectedUri();
        if (connectedUri != null) {
            networkCategory.addEntry(entryBuilder.startTextDescription(
                    Component.translatable("option.ghostmod.connectedUri", connectedUri.toString()))
                    .build());
        }

        // 接続/切断ボタン
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
                serverPortEntry.getValue(),
                serverPasswordEntry.getValue());
    }

    private void connectPushButton(boolean connected, Screen parent) {
        if (!Objects.equals(connected, this.pre_connected)) {
            LogUtils.getLogger().info("toggle state connected: {}", connected);
            if (connected) {
                // 現在のUI値からスナップショットを作成
                GhostConfig.ConfigSnapshot currentSnapshot = createCurrentSnapshot();
                try {
                    URI uri = new URI(currentSnapshot.getFullWebSocketUri());
                    var isConnected = GhostModClient.GHOST_SYNC_SERVICE.connectBlocking(uri,
                            currentSnapshot.serverPassword(), 3, TimeUnit.SECONDS);
                    if (isConnected) {
                        // 接続成功: トースト通知のみ、保存はしない（保存は明示的にSaveボタンで行う）
                        showConnectionSuccessToast();
                        pendingSnapshot = currentSnapshot;
                        LogUtils.getLogger().info("Successfully connected to server.");
                    } else {
                        // 接続失敗時: 入力値を保持して画面リフレッシュ後も維持
                        pendingSnapshot = currentSnapshot;
                        LogUtils.getLogger().error("Failed to connect server.");
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
