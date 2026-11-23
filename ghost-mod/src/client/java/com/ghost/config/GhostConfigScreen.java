package com.ghost.config;

import com.ghost.GhostModClient;
import com.ghost.client.ToastNotifications;
import com.ghost.net.GhostSyncService;
import com.mojang.logging.LogUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.URI;
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
    @Nullable
    private GhostConfig.ConfigSnapshot pendingSnapshot;

    private StringListEntry serverUriEntry;
    private IntegerListEntry serverPortEntry;
    private BooleanListEntry useTlsEntry;
    private StringListEntry serverPasswordEntry;
    private Screen activeScreen;

    private GhostConfigScreen(GhostSyncService syncService,
                              GhostConfig config,
                              @Nullable GhostConfig.ConfigSnapshot pendingSnapshot) {
        this.syncService = syncService;
        this.config = config;
        this.pendingSnapshot = pendingSnapshot;
    }

    /**
     * 設定画面を作成します（静的ファクトリメソッド）。
     *
     * @param parent 親画面
     * @return 設定画面
     */
    public static Screen create(Screen parent) {
        return new GhostConfigScreen(
                GhostModClient.getSyncService(),
                GhostConfig.getInstance(),
                null
        ).buildScreen(parent);
    }

    private Screen buildScreen(Screen parent) {
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.ghostmod.config"))
                .setDoesConfirmSave(false)
                .setAlwaysShowTabs(false)
                .setSavingRunnable(() -> {
                    config.save();
                    pendingSnapshot = null;
                });

        final ConfigCategory networkCategory = builder
                .getOrCreateCategory(Component.translatable("category.ghostmod.network"));
        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // --- サーバー設定 ---
        serverUriEntry = entryBuilder.startStrField(
                        Component.translatable("option.ghostmod.serverUri"), config.getServerUri())
                .setDefaultValue("localhost")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverUri"))
                .setSaveConsumer(config::setServerUri)
                .build();
        if (pendingSnapshot != null) {
            setTextFieldValue(serverUriEntry, pendingSnapshot.serverUri());
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
        if (pendingSnapshot != null) {
            setTextFieldValue(serverPortEntry, String.valueOf(pendingSnapshot.serverPort()));
        }
        networkCategory.addEntry(serverPortEntry);

        boolean initialUseTls = pendingSnapshot != null ? pendingSnapshot.useTls() : config.isUseTls();
        useTlsEntry = entryBuilder.startBooleanToggle(
                        Component.translatable("option.ghostmod.useTls"), initialUseTls)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("tooltip.ghostmod.useTls"))
                .setSaveConsumer(config::setUseTls)
                .build();
        networkCategory.addEntry(useTlsEntry);

        serverPasswordEntry = entryBuilder.startStrField(
                        Component.translatable("option.ghostmod.serverPassword"), config.getServerPassword())
                .setDefaultValue("changeme")
                .setTooltip(Component.translatable("tooltip.ghostmod.serverPassword"))
                .setSaveConsumer(config::setServerPassword)
                .build();
        applyPasswordMask(serverPasswordEntry);
        if (pendingSnapshot != null) {
            setTextFieldValue(serverPasswordEntry, pendingSnapshot.serverPassword());
        }
        networkCategory.addEntry(serverPasswordEntry);

        // --- 接続管理 ---
        final boolean initialConnected = syncService.isConnected();

        networkCategory.addEntry(entryBuilder.startTextDescription(
                        Component.translatable("option.ghostmod.status", getStatusText(initialConnected)))
                .build());

        URI connectedUri = syncService.getConnectedUri();
        if (connectedUri != null) {
            networkCategory.addEntry(entryBuilder.startTextDescription(
                            Component.translatable("option.ghostmod.connectedUri", connectedUri.toString()))
                    .build());
        }

        // トグル押下を 1 回だけ検知し、描画外（Minecraft#execute）から非同期接続／切断を開始する
        final AtomicBoolean actionTriggered = new AtomicBoolean(false);
        var reconnectButton = entryBuilder.startBooleanToggle(
                        Component.translatable("option.ghostmod.connection"), initialConnected)
                .setYesNoTextSupplier(toggleState -> {
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
        reconnectButton.setEditable(Minecraft.getInstance().hasSingleplayerServer());
        networkCategory.addEntry(reconnectButton);

        this.activeScreen = builder.build();
        return this.activeScreen;
    }

    private void triggerConnectionChange(boolean connect,
                                         GhostConfig.ConfigSnapshot snapshot,
                                         Screen parent) {
        final Screen screenBeforeAction = this.activeScreen;
        this.pendingSnapshot = snapshot;

        if (connect) {
            URI uri;
            try {
                uri = snapshot.toWebSocketUri();
            } catch (Exception ex) {
                LogUtils.getLogger().error("Invalid server URI: host='{}', port={}, useTls={}",
                        snapshot.serverUri(), snapshot.serverPort(), snapshot.useTls(), ex);
                refreshIfStillShowing(screenBeforeAction, parent);
                return;
            }

            syncService.connectAsync(uri, snapshot.serverPassword(), 3, TimeUnit.SECONDS)
                    .whenComplete((connected, error) -> Minecraft.getInstance().execute(() -> {
                        if (error != null) {
                            LogUtils.getLogger().error("Connection error:", error);
                        } else if (Boolean.TRUE.equals(connected)) {
                            ToastNotifications.showConnectionSuccessToast();
                            LogUtils.getLogger().info("Successfully connected to server.");
                        } else {
                            LogUtils.getLogger().error("Failed to connect to server.");
                        }
                        refreshIfStillShowing(screenBeforeAction, parent);
                    }));
        } else {
            CompletableFuture.runAsync(syncService::disconnect)
                    .whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
                        if (error != null) {
                            LogUtils.getLogger().error("Disconnect error:", error);
                        }
                        refreshIfStillShowing(screenBeforeAction, parent);
                    }));
        }
    }

    private void refreshIfStillShowing(Screen expectedScreen, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (getCurrentScreen(mc) == expectedScreen) {
            Screen refreshed = new GhostConfigScreen(syncService, config, pendingSnapshot).buildScreen(parent);
            openScreen(mc, refreshed);
        }
    }

    private Component getStatusText(boolean isConnected) {
        return Component
                .translatable(isConnected ? "text.ghostmod.status.connected" : "text.ghostmod.status.disconnected")
                .withStyle(style -> style.withColor(isConnected ? 0x55FF55 : 0xFF5555));
    }

    private GhostConfig.ConfigSnapshot captureCurrentSnapshot() {
        String uriValue = serverUriEntry.getValue();
        Integer portValue = serverPortEntry.getValue();
        Boolean tlsValue = useTlsEntry.getValue();
        String passwordValue = serverPasswordEntry.getValue();

        String safeUri = (uriValue != null && !uriValue.isBlank()) ? uriValue : config.getServerUri();
        int safePort = (portValue != null && portValue >= 1 && portValue <= 65535) ? portValue : config.getServerPort();
        boolean safeTls = tlsValue != null ? tlsValue : config.isUseTls();
        String safePassword = passwordValue != null ? passwordValue : config.getServerPassword();

        return new GhostConfig.ConfigSnapshot(safeUri, safePort, safeTls, safePassword);
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
