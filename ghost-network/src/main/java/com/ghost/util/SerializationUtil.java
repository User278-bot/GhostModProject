package com.ghost.util;

import com.ghost.api.dto.AuthData;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.dto.PlayerData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class SerializationUtil {
    private static final Gson GSON = new Gson();

    public static Gson getGson() {
        return GSON;
    }

    private SerializationUtil() {
    }

    // --- GhostPacketのシリアライズ ---
    public static <T> String serializePacket(GhostPacket<T> packet) {
        return GSON.toJson(packet);
    }

    public static GhostPacket<JsonElement> deserializePacket(String msg) {
        Type type = new TypeToken<GhostPacket<JsonElement>>() {
        }.getType();
        return GSON.fromJson(msg, type);
    }

    public static PlayerData parsePlayerData(JsonElement element) {
        return GSON.fromJson(element, PlayerData.class);
    }

    public static AuthData parseAuthData(JsonElement element) {
        return GSON.fromJson(element, AuthData.class);
    }

    public static String parseUUID(JsonElement element) {
        return GSON.fromJson(element, String.class);
    }
}
