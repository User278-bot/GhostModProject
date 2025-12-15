package com.ghost.server;// ClientSession.java

import com.ghost.api.dto.PlayerData;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record GhostClientData(String nonce, boolean isAuthenticated, PlayerData playerData,
                              Set<String> trackedPlayers) {
    public GhostClientData(String nonce, boolean isAuthenticated, PlayerData playerData) {
        this(nonce, isAuthenticated, playerData, ConcurrentHashMap.newKeySet());
    }

}
