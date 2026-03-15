package com.ghost.init;

import com.ghost.config.GhostConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?}
import org.lwjgl.glfw.GLFW;

/**
 * Ghost Modのキーバインドを登録するクラス。
 */
public class GhostModKeyBindings {
    // 設定画面を開くキーバインド（デフォルト: Gキー）
    private static KeyMapping openConfigKey;

    /**
     * キーバインドを登録します。
     * ClientModInitializerのonInitializeClientから呼び出してください。
     */
    public static void register() {
        // キーバインドの登録
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ghostmod.openConfig", // 翻訳キー
                InputConstants.Type.KEYSYM, // キーボード入力
                GLFW.GLFW_KEY_G, // デフォルト: Gキー
                //? if >=1.21.11 {
                /*KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ghostmod","keys"))
                *///?} else {
                "category.ghostmod.keys" 
                //?}
                // カテゴリ翻訳キー
        ));

        // キー押下時のイベントハンドラを登録
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                // ワールドに入っている場合のみ設定画面を開く
                if (client.level != null) {
                    Minecraft.getInstance().setScreen(GhostConfigScreen.create(client.screen));
                }
            }
        });
    }
}
