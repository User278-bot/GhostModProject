package com.ghost.util;

import com.ghost.net.GhostPacket;
import com.ghost.net.MessageType;
import com.ghost.common.dto.PlayerData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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

    private static String serialize(PlayerData data) {
        return GSON.toJson(data);
    }

    // --- GhostPacketのシリアライズ ---
    public static <T> String serializePacket(GhostPacket<T> packet) {
        return GSON.toJson(packet);
    }

    // --- GhostPacketのデシリアライズ ---
    // PlayerData用
    public static GhostPacket<PlayerData> deserializeUpdatePacket(String json) {
        Type packetType = new TypeToken<GhostPacket<PlayerData>>() {
        }.getType();
        return GSON.fromJson(json, packetType);
    }

    // LEAVEメッセージ用 (データはUUID文字列)
    public static GhostPacket<String> deserializeLeavePacket(String json) {
        Type packetType = new TypeToken<GhostPacket<String>>() {
        }.getType();
        return GSON.fromJson(json, packetType);
    }

    // INITIAL_SYNCメッセージ用 (データはPlayerDataのリスト)
    public static GhostPacket<List<PlayerData>> deserializeInitialSyncPacket(String json) {
        Type packetType = new TypeToken<GhostPacket<List<PlayerData>>>() {
        }.getType();
        return GSON.fromJson(json, packetType);
    }

    // まずはメッセージタイプだけを調べるためのヘルパー
    public static MessageType peekMessageType(String json) {
        try {
            GhostPacket<?> packet = GSON.fromJson(json, GhostPacket.class);
            return packet.getType();
        } catch (Exception ex) {
            LOGGER.error("MessageType error", ex);
            return null;
        }
    }

    private static PlayerData deserialize(String js) {
        try {
            return GSON.fromJson(js, PlayerData.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.error("Failed to deserialize Json.", ex);
            return null;
        }
    }
}
