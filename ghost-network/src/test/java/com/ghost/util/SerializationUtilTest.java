package com.ghost.util;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.dto.Vec2Dto;
import com.ghost.common.dto.Vec3Dto;
import com.ghost.net.packet.GhostPacket;
import com.ghost.net.packet.MessageType;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerializationUtilTest {

    @Test
    void serializeAndDeserialize_ShouldPreserveData() {
        // 準備
        PlayerData originalData = new PlayerData(
                new Vec3Dto(1.0, 2.0, 3.0),
                new Vec2Dto(90.0f, 45.0f),
                "uuid-123",
                "TestPlayer",
                "STANDING",
                "minecraft:overworld");
        GhostPacket<PlayerData> packet = new GhostPacket<>(MessageType.UPDATE, originalData);

        // 実行: シリアライズ
        String json = SerializationUtil.serializePacket(packet);
        assertNotNull(json);

        // 実行: デシリアライズ
        GhostPacket<JsonElement> deserializedPacket = SerializationUtil.deserializePacket(json);
        assertNotNull(deserializedPacket);
        assertEquals(MessageType.UPDATE, deserializedPacket.getType());

        // データ部分のパース
        PlayerData parsedData = SerializationUtil.parsePlayerData(deserializedPacket.getData());

        // 検証
        assertEquals(originalData.uuid(), parsedData.uuid());
        assertEquals(originalData.name(), parsedData.name());
        assertEquals(originalData.pos().x(), parsedData.pos().x(), 0.000001);
    }

    @Test
    void parseUUID_ShouldParseStringData() {
        // 準備
        String uuid = "uuid-leave-123";
        GhostPacket<String> packet = new GhostPacket<>(MessageType.LEAVE, uuid);
        String json = SerializationUtil.serializePacket(packet);

        // 実行
        GhostPacket<JsonElement> deserializedPacket = SerializationUtil.deserializePacket(json);
        String parsedUuid = SerializationUtil.parseUUID(deserializedPacket.getData());

        // 検証
        assertEquals(uuid, parsedUuid);
    }
}
