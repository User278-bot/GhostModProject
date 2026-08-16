package com.ghost.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.Component;

public class ToastNotifications {
    /**
     * 接続成功を通知するトーストを表示します。
     */
    public static void showConnectionSuccessToast() {
        Minecraft.getInstance()
                //? if >= 26.2{
                /*.gui.toastManager()
                .addToast(
                        new SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.translatable("toast.ghostmod.connected.title"),
                                Component.translatable("toast.ghostmod.connected.description")
                        )
                )
                *///? } elif >= 1.21.4 {
        //      .getToastManager()
        //      .addToast(
        //                SystemToast.multiline(
        //                        Minecraft.getInstance(),
        //                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
        //                        Component.translatable("toast.ghostmod.connected.title"),
        //                        Component.translatable("toast.ghostmod.connected.description")))
                //? } elif >= 1.20.5 {
        //      .getToasts()
        //      .addToast(
        //                SystemToast.multiline(
        //                        Minecraft.getInstance(),
        //                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
        //                        Component.translatable("toast.ghostmod.connected.title"),
        //                        Component.translatable("toast.ghostmod.connected.description")))
        //
                //? } else {
              .getToasts()
              .addToast(
                        SystemToast.multiline(
                                Minecraft.getInstance(),
                                SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                                Component.translatable("toast.ghostmod.connected.title"),
                                Component.translatable("toast.ghostmod.connected.description")))
                //? }
        ;

    }

}
