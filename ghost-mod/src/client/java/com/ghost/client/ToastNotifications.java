package com.ghost.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ToastNotifications {
    /**
     * 接続成功を通知するトーストを表示します。
     */
    public static void showConnectionSuccessToast() {
        Minecraft.getInstance()
                //? if >=1.21.4 {
                /*.getToastManager()
                *///?} else {
                .getToasts()
                //?}
                .addToast(
                SystemToast.multiline(
                        Minecraft.getInstance(),
                        /*? >=1.20.6 {*/
                        
                        /*SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                         
                        *//*?} else {*/
                        SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, //?}
                        Component.translatable("toast.ghostmod.connected.title"),
                        Component.translatable("toast.ghostmod.connected.description")));
    }
}
