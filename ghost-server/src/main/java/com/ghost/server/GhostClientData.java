package com.ghost.server;

import com.ghost.api.dto.PlayerData;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側で保持するクライアントセッション状態。
 */
public record GhostClientData(
        @Nullable String nonce,
        boolean isAuthenticated,
        @Nullable PlayerData playerData,
        Set<String> trackedPlayers
) {
    public GhostClientData(@Nullable String nonce, boolean isAuthenticated, @Nullable PlayerData playerData) {
        this(nonce, isAuthenticated, playerData, ConcurrentHashMap.newKeySet());
    }

    /** 未認証の初期セッション状態を作成します。 */
    public static GhostClientData unauthenticated(String nonce) {
        return new GhostClientData(nonce, false, null);
    }

    /** 認証完了直後のセッション状態を作成します（nonce は破棄）。 */
    public static GhostClientData authenticated() {
        return new GhostClientData(null, true, null);
    }

    /** 追跡セットを引き継いだまま最新の PlayerData を反映した状態を返します。 */
    public GhostClientData withPlayerData(PlayerData newPlayerData) {
        return new GhostClientData(nonce, isAuthenticated, newPlayerData, trackedPlayers);
    }
}
