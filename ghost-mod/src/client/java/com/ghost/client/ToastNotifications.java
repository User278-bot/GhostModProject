package com.ghost.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class ToastNotifications {
    private ToastNotifications() {
    }

    /**
     * 接続成功を通知するトーストを表示します。
     */
    public static void showConnectionSuccessToast() {
        Minecraft client = Minecraft.getInstance();
        Component title = Component.translatable("toast.ghostmod.connected.title");
        Component description = Component.translatable("toast.ghostmod.connected.description");

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
