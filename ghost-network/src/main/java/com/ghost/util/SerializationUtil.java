package com.ghost.util;

import com.ghost.api.packet.GhostPacket;
import com.ghost.api.dto.PlayerData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public final class SerializationUtil {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationUtil.class);

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

    public static String parseUUID(JsonElement element) {
        return GSON.fromJson(element, String.class);
    }

    public static List<PlayerData> parsePlayerDataList(JsonElement element) {
        Type listType = new TypeToken<List<PlayerData>>() {
        }.getType();
        return GSON.fromJson(element, listType);
    }
}
