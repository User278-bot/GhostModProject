package com.ghost.server;// ClientSession.java

import com.ghost.api.dto.PlayerData;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GhostClientData {
    private final String nonce;
    private final boolean isAuthenticated;
    private final PlayerData playerData;
    private final Set<String> trackedPlayers;

    public GhostClientData(String nonce, boolean isAuthenticated, PlayerData playerData) {
        this(nonce, isAuthenticated, playerData, ConcurrentHashMap.newKeySet());
    }

    public GhostClientData(String nonce, boolean isAuthenticated, PlayerData playerData, Set<String> trackedPlayers) {
        this.nonce = nonce;
        this.isAuthenticated = isAuthenticated;
        this.playerData = playerData;
        this.trackedPlayers = trackedPlayers;
    }

    public String nonce() {
        return nonce;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public PlayerData playerData() {
        return playerData;
    }

    public Set<String> trackedPlayers() {
        return trackedPlayers;
    }
}
