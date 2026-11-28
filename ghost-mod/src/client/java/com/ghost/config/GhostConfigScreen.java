package com.ghost.config;

import com.ghost.GhostModClient;
import com.ghost.client.ToastNotifications;
import com.ghost.net.ConnectionResult.FailureReason;
import com.ghost.net.GhostSyncService;
import com.ghost.util.WebSocketUriBuilder;
import com.ghost.util.WebSocketUriBuilder.UrlValidationError;
import com.mojang.logging.LogUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ghost Mod の設定画面を作成するクラス。
 * Cloth Config API を使用し、ModMenu に依存しません。
 * 接続／切断処理は非同期に実行し、メイン描画スレッドをブロックしない設計とします。
 */
public final class GhostConfigScreen {

    @Nullable
    private static final Field TEXT_FIELD_WIDGET_FIELD = resolveTextFieldWidgetField();

    private final GhostSyncService syncService;
    private final GhostConfig config;
    private final GhostConfig.ConfigSnapshot initialSavedSnapshot;
    @Nullable
    private GhostConfig.ConfigSnapshot pendingSnapshot;
    @Nullable
    private FailureReason lastFailureReason;
    private boolean connecting;
    private boolean manuallyDisconnected;

    private StringListEntry serverUriEntry;
    private IntegerListEntry serverPortEntry;
    private StringListEntry serverPasswordEntry;
    private Screen activeScreen;

    private GhostConfigScreen(GhostSyncService syncService,
                              GhostConfig config,
                              GhostConfig.ConfigSnapshot initialSavedSnapshot,
                              @Nullable GhostConfig.ConfigSnapshot pendingSnapshot,
                              @Nullable FailureReason lastFailureReason,
                              boolean connecting,
                              boolean manuallyDisconnected) {
        this.syncService = syncService;
        this.config = config;
        this.initialSavedSnapshot = initialSavedSnapshot;
        this.pendingSnapshot = pendingSnapshot;
        this.lastFailureReason = lastFailureReason;
        this.connecting = connecting;
        this.manuallyDisconnected = manuallyDisconnected;
    }

    /**
     * 設定画面を作成します（静的ファクトリメソッド）。
     *
     * @param parent 親画面
     * @return 設定画面
     */
    public static Screen create(Screen parent) {
        GhostConfig cfg = GhostConfig.getInstance();
        return new GhostConfigScreen(
                GhostModClient.getSyncService(),
                cfg,
                cfg.toSnapshot(),
                null,
                null,
                false,
                false
        ).buildScreen(parent);
    }

    private Screen buildScreen(Screen parent) {
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.ghostmod.config"))
                .setDoesConfirmSave(false)
                .setAlwaysShowTabs(false)
                .setSavingRunnable(this::onSaveConfig);

        final ConfigCategory networkCategory = builder
                .getOrCreateCategory(Component.translatable("category.ghostmod.network"));
        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // --- 1. 接続ステータス表示 ---
        final boolean initialConnected = syncService.isConnected();
        final boolean inSingleplayer = Minecraft.getInstance().hasSingleplayerServer();

        networkCategory.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("option.ghostmod.status", getStatusText(initialConnected)))
                .build());

        URI connectedUri = syncService.getConnectedUri();
        if (!connecting && initialConnected && connectedUri != null) {
            networkCategory.addEntry(entryBuilder.startTextDescription(
                            Component.translatable("option.ghostmod.connectedUri", connectedUri.toString())
                                    .withStyle(ChatFormatting.GRAY))
                    .build());
        } else if (!connecting && !initialConnected && lastFailureReason != null) {
            networkCategory.addEntry(entryBuilder.startTextDescription(
                            Component.translatable(
                                            "option.ghostmod.failureReason",
                                            ToastNotifications.getFailureReasonComponent(lastFailureReason))
                                    .withStyle(ChatFormatting.RED))
                    .build());
        }

        // --- 2. 接続コントロール ---
        // トグル押下を 1 回だけ検知し、描画外（Minecraft#execute）から非同期接続／切断を開始する
        final AtomicBoolean actionTriggered = new AtomicBoolean(false);
        var reconnectButton = entryBuilder.startBooleanToggle(
                        Component.translatable("option.ghostmod.connection"), initialConnected)
                .setTooltip(Component.translatable(
                        inSingleplayer
                                ? "tooltip.ghostmod.connection"
                                : "tooltip.ghostmod.connection.unavailable"))
                .setYesNoTextSupplier(toggleState -> {
                    if (connecting) {
                        return Component.translatable("option.ghostmod.connection.connecting")
                                .withStyle(ChatFormatting.YELLOW);
                    }
                    if (toggleState != initialConnected && actionTriggered.compareAndSet(false, true)) {
                        GhostConfig.ConfigSnapshot snapshot = captureCurrentSnapshot();
                        Minecraft.getInstance().execute(() ->
                                triggerConnectionChange(toggleState, snapshot, parent));
                    }
                    return Component.translatable(
                            initialConnected
                                    ? "option.ghostmod.connection.disconnect"
                                    : "option.ghostmod.connection.connect");
                })
                .build();
        reconnectButton.setEditable(inSingleplayer && !connecting);
        networkCategory.addEntry(reconnectButton);

        // --- 3. サーバー設定（URL / ポート / パスワード） ---
        serverUriEntry = entryBuilder.startStrField(
                        Component.translatable("option.ghostmod.serverUri"), config.getServerUri())
                .setDefaultValue(GhostConfig.DEFAULT_SERVER_URI)
                .setTooltip(
                        Component.translatable("tooltip.ghostmod.serverUri.line1"),
                        Component.translatable("tooltip.ghostmod.serverUri.line2"))
                .setErrorSupplier(GhostConfigScreen::validateServerUri)
                .setSaveConsumer(val -> config.setServerUri(val != null ? val.trim() : GhostConfig.DEFAULT_SERVER_URI))
                .build();
        if (pendingSnapshot != null) {
            setTextFieldValue(serverUriEntry, pendingSnapshot.serverUri());
        }
        networkCategory.addEntry(serverUriEntry);

        serverPortEntry = entryBuilder.startIntField(
                        Component.translatable("option.ghostmod.serverPort"), config.getServerPort())
                .setDefaultValue(GhostConfig.DEFAULT_SERVER_PORT)
                .setMin(1)
                .setMax(65535)
                .setTooltip(
                        Component.translatable("tooltip.ghostmod.serverPort.line1"),
                        Component.translatable("tooltip.ghostmod.serverPort.line2"))
                .setSaveConsumer(config::setServerPort)
                .build();
        if (pendingSnapshot != null) {
            setTextFieldValue(serverPortEntry, String.valueOf(pendingSnapshot.serverPort()));
        }
        networkCategory.addEntry(serverPortEntry);

        serverPasswordEntry = entryBuilder.startStrField(
                        Component.translatable("option.ghostmod.serverPassword"), config.getServerPassword())
                .setDefaultValue(GhostConfig.DEFAULT_SERVER_PASSWORD)
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPassword"))
                .setSaveConsumer(config::setServerPassword)
                .build();
        applyPasswordMask(serverPasswordEntry);
        if (pendingSnapshot != null) {
            setTextFieldValue(serverPasswordEntry, pendingSnapshot.serverPassword());
        }
        networkCategory.addEntry(serverPasswordEntry);

        this.activeScreen = builder.build();
        return this.activeScreen;
    }

    private void onSaveConfig() {
        config.save();
        pendingSnapshot = null;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer() || connecting) {
            return;
        }

        GhostConfig.ConfigSnapshot savedSnapshot = config.toSnapshot();
        boolean configChanged = !savedSnapshot.equals(initialSavedSnapshot);

        final URI targetUri;
        try {
            targetUri = savedSnapshot.toWebSocketUri();
        } catch (Exception ex) {
            LogUtils.getLogger().error("Invalid server configuration on save.", ex);
            ToastNotifications.showConnectionFailureToast(FailureReason.INVALID_URI);
            return;
        }

        boolean uriDiffersFromActive = syncService.isConnected()
                && !targetUri.equals(syncService.getConnectedUri());

        // シングルプレイ中に設定が変更された場合（または保存値と現在の接続先が異なる場合）は自動で（再）接続する
        if (configChanged || uriDiffersFromActive) {
            syncService.connectWithResultAsync(targetUri, savedSnapshot.serverPassword(), 3, TimeUnit.SECONDS)
                    .whenComplete((result, error) -> mc.execute(() -> {
                        if (error != null) {
                            LogUtils.getLogger().error("Connection error after saving config:", error);
                            ToastNotifications.showConnectionFailureToast(FailureReason.CONNECTION_ERROR);
                        } else if (result != null && result.connected()) {
                            ToastNotifications.showConnectionSuccessToast(targetUri);
                        } else {
                            FailureReason reason = result != null
                                    ? result.failureReason()
                                    : FailureReason.CONNECTION_ERROR;
                            ToastNotifications.showConnectionFailureToast(reason);
                        }
                    }));
        }
    }

    private static Optional<Component> validateServerUri(String value) {
        return WebSocketUriBuilder.validateUrlInput(value)
                .map(GhostConfigScreen::toValidationErrorMessage);
    }

    private static Component toValidationErrorMessage(UrlValidationError error) {
        String key = switch (error) {
            case EMPTY -> "error.ghostmod.url.empty";
            case UNSUPPORTED_SCHEME -> "error.ghostmod.url.unsupportedScheme";
            case PORT_IN_URL -> "error.ghostmod.url.portInUrl";
            case INVALID_HOST -> "error.ghostmod.url.invalidHost";
        };
        return Component.translatable(key);
    }

    private void triggerConnectionChange(boolean connect,
                                         GhostConfig.ConfigSnapshot snapshot,
                                         Screen parent) {
        final Screen screenBeforeAction = this.activeScreen;
        this.pendingSnapshot = snapshot;

        if (connect) {
            this.manuallyDisconnected = false;
            final URI uri;
            try {
                uri = snapshot.toWebSocketUri();
            } catch (Exception ex) {
                LogUtils.getLogger().error("Invalid server URI: url='{}', port={}",
                        snapshot.serverUri(), snapshot.serverPort(), ex);
                this.lastFailureReason = FailureReason.INVALID_URI;
                ToastNotifications.showConnectionFailureToast(FailureReason.INVALID_URI);
                refreshIfStillShowing(screenBeforeAction, parent);
                return;
            }

            this.connecting = true;
            this.lastFailureReason = null;
            final Screen connectingScreen = refreshIfStillShowing(screenBeforeAction, parent);
            final Screen screenToWatch = connectingScreen != null ? connectingScreen : screenBeforeAction;

            syncService.connectWithResultAsync(uri, snapshot.serverPassword(), 3, TimeUnit.SECONDS)
                    .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                        this.connecting = false;
                        if (error != null) {
                            LogUtils.getLogger().error("Connection error:", error);
                            this.lastFailureReason = FailureReason.CONNECTION_ERROR;
                            ToastNotifications.showConnectionFailureToast(FailureReason.CONNECTION_ERROR);
                        } else if (result != null && result.connected()) {
                            this.lastFailureReason = null;
                            ToastNotifications.showConnectionSuccessToast(uri);
                            LogUtils.getLogger().info("Successfully connected to server: {}", uri);
                        } else {
                            FailureReason reason = result != null
                                    ? result.failureReason()
                                    : FailureReason.CONNECTION_ERROR;
                            this.lastFailureReason = reason;
                            ToastNotifications.showConnectionFailureToast(reason);
                            LogUtils.getLogger().warn("Failed to connect to server: {} (reason={})", uri, reason);
                        }
                        refreshIfStillShowing(screenToWatch, parent);
                    }));
        } else {
            this.manuallyDisconnected = true;
            this.lastFailureReason = null;
            CompletableFuture.runAsync(syncService::disconnect)
                    .whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
                        if (error != null) {
                            LogUtils.getLogger().error("Disconnect error:", error);
                        }
                        refreshIfStillShowing(screenBeforeAction, parent);
                    }));
        }
    }

    @Nullable
    private Screen refreshIfStillShowing(Screen expectedScreen, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (getCurrentScreen(mc) == expectedScreen) {
            Screen refreshed = new GhostConfigScreen(
                    syncService,
                    config,
                    initialSavedSnapshot,
                    pendingSnapshot,
                    lastFailureReason,
                    connecting,
                    manuallyDisconnected
            ).buildScreen(parent);
            openScreen(mc, refreshed);
            return refreshed;
        }
        return null;
    }

    private Component getStatusText(boolean isConnected) {
        if (connecting) {
            return Component.translatable("text.ghostmod.status.connecting")
                    .withStyle(style -> style.withColor(0xFFFF55));
        }
        return Component
                .translatable(isConnected ? "text.ghostmod.status.connected" : "text.ghostmod.status.disconnected")
                .withStyle(style -> style.withColor(isConnected ? 0x55FF55 : 0xFF5555));
    }

    private GhostConfig.ConfigSnapshot captureCurrentSnapshot() {
        String uriValue = serverUriEntry.getValue();
        Integer portValue = serverPortEntry.getValue();
        String passwordValue = serverPasswordEntry.getValue();

        String safeUri = uriValue != null ? uriValue.trim() : config.getServerUri();
        int safePort = (portValue != null && portValue >= 1 && portValue <= 65535) ? portValue : config.getServerPort();
        String safePassword = passwordValue != null ? passwordValue : config.getServerPassword();

        return new GhostConfig.ConfigSnapshot(safeUri, safePort, safePassword);
    }

    @Nullable
    private static Field resolveTextFieldWidgetField() {
        try {
            Field field = TextFieldListEntry.class.getDeclaredField("textFieldWidget");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to access TextFieldListEntry.textFieldWidget", e);
            return null;
        }
    }

    @Nullable
    private static EditBox getEditBox(TextFieldListEntry<?> entry) {
        if (TEXT_FIELD_WIDGET_FIELD == null) {
            return null;
        }
        try {
            return (EditBox) TEXT_FIELD_WIDGET_FIELD.get(entry);
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to retrieve EditBox from TextFieldListEntry", e);
            return null;
        }
    }

    private static void setTextFieldValue(TextFieldListEntry<?> entry, String value) {
        EditBox widget = getEditBox(entry);
        if (widget != null) {
            widget.setValue(value);
        }
    }

    private static void applyPasswordMask(TextFieldListEntry<?> entry) {
        EditBox widget = getEditBox(entry);
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
    }

    @Nullable
    private static Screen getCurrentScreen(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.screen();
        *///?} else {
        return mc.screen;
        //?}
    }

    private static void openScreen(Minecraft mc, Screen screen) {
        //? if >=26.2 {
        /*mc.setScreenAndShow(screen);
        *///?} else {
        mc.setScreen(screen);
        //?}
    }
}
