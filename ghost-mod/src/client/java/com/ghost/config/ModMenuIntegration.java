package com.ghost.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu統合クラス。
 * ModMenuがインストールされている場合のみ読み込まれます。
 * 設定画面の作成はGhostConfigScreenに委譲します。
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GhostConfigScreen::create;
    }
}