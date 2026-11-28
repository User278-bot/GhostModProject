package com.ghost.client;

import com.ghost.net.ConnectionResult.FailureReason;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.net.URI;

/**
 * 接続完了・接続失敗・予期せぬ切断をユーザーへ通知するトースト表示ユーティリティ。
 */
public final class ToastNotifications {
    private ToastNotifications() {
    }

    /**
     * 接続成功を通知するトーストを表示します。
     *
     * @param connectedUri 接続に成功したサーバー URI
     */
    public static void showConnectionSuccessToast(URI connectedUri) {
        Component title = Component.translatable("toast.ghostmod.connected.title");
        Component description = Component.translatable(
                "toast.ghostmod.connected.description",
                connectedUri.toString());
        showToast(title, description);
    }

    /**
     * 接続失敗を原因別に通知するトーストを表示します。
     *
     * @param reason 失敗原因
     */
    public static void showConnectionFailureToast(FailureReason reason) {
        Component title = Component.translatable("toast.ghostmod.connectFailed.title")
                .withStyle(ChatFormatting.RED);
        Component description = getFailureReasonComponent(reason);
        showToast(title, description);
    }

    /**
     * 通信確立後にサーバー側から予期せず切断されたことを通知するトーストを表示します。
     */
    public static void showUnexpectedDisconnectToast() {
        Component title = Component.translatable("toast.ghostmod.disconnected.title")
                .withStyle(ChatFormatting.GOLD);
        Component description = Component.translatable("toast.ghostmod.disconnected.description");
        showToast(title, description);
    }

    /**
     * {@link FailureReason} に対応するローカライズ済み説明テキストを返します。
     */
    public static Component getFailureReasonComponent(FailureReason reason) {
        String key = switch (reason) {
            case INVALID_URI -> "text.ghostmod.error.invalidUri";
            case UNREACHABLE -> "text.ghostmod.error.unreachable";
            case TLS_ERROR -> "text.ghostmod.error.tls";
            case AUTH_FAILED -> "text.ghostmod.error.authFailed";
            case PROTOCOL_MISMATCH -> "text.ghostmod.error.protocolMismatch";
            case CONNECTION_ERROR, NONE -> "text.ghostmod.error.connectionFailed";
        };
        return Component.translatable(key);
    }

    private static void showToast(Component title, Component description) {
        Minecraft client = Minecraft.getInstance();

        //? if >= 26.2 {
        /*client.gui.toastManager().addToast(
                new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, description)
        );
        *///?} elif >= 1.21.4 {
        /*client.getToastManager().addToast(
                SystemToast.multiline(client, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, description)
        );
        *///?} elif >= 1.20.5 {
        /*client.getToasts().addToast(
                SystemToast.multiline(client, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, description)
        );
        *///?} else {
        client.getToasts().addToast(
                SystemToast.multiline(client, SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, title, description)
        );
        //?}
    }
}
