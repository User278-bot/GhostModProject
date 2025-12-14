package com.ghost.server;// ClientSession.java

import com.ghost.api.dto.PlayerData;

public record GhostClientData(String nonce, boolean isAuthenticated,PlayerData playerData) {
}

