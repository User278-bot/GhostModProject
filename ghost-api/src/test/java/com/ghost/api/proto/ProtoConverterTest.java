package com.ghost.api.proto;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.item.components.CustomModelDataDto;
import com.ghost.api.dto.item.components.TrimDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ProtoConverter} の oneof 全パケット種別および DTO 往復変換を検証するユニットテスト。
 */
class ProtoConverterTest {

    @Test
    void updatePacket_RoundTripPreservesAllFieldsIncludingCustomModelDataModel() throws Exception {
        ItemDto head = new ItemDto.Builder("minecraft:diamond_helmet")
                .damage(10)
                .hasGlint(true)
                .color(0x00FF00)
                .trim(new TrimDto("coast", "diamond"))
                .customModelData(new CustomModelDataDto(
                        List.of(1.0f, 2.5f), List.of(true), List.of("model1"), List.of(12345), 777))
                .itemModel("minecraft:custom_helmet")
                .build();

        EquipmentDto equipment = new EquipmentDto(
                ItemDto.ITEM_AIR,
                ItemDto.ITEM_AIR,
                head,
                ItemDto.ITEM_AIR,
                ItemDto.ITEM_AIR,
                ItemDto.ITEM_AIR
        );

        PlayerData original = new PlayerData(
                new Vec3Dto(100.5, 64.0, -200.75),
                new Vec2Dto(45.0f, -90.0f),
                "12345678-1234-1234-1234-123456789abc",
                "TestPlayer",
                "STANDING",
                "minecraft:overworld",
                (byte) 127,
                "RIGHT",
                5,
                equipment,
                true,
                "MAIN_HAND"
        );

        byte[] serialized = ProtoConverter.encodeUpdate(original);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        // 1 パスデコードで UPDATE 種別と全フィールドが復元されること
        GhostPacketProto decoded = ProtoConverter.decode(serialized);
        assertEquals(GhostPacketProto.PayloadCase.UPDATE, decoded.getPayloadCase());

        PlayerData restored = ProtoConverter.fromProto(decoded.getUpdate());
        assertEquals(original, restored);
        assertNotNull(restored.equipment().head().customModelData());
        assertEquals(777, restored.equipment().head().customModelData().model());
    }

    @Test
    void leaveAndDespawnPackets_RoundTrip() throws Exception {
        String uuid = "abcdef12-3456-7890-abcd-ef1234567890";

        GhostPacketProto leavePacket = ProtoConverter.decode(ProtoConverter.encodeLeave(uuid));
        assertEquals(GhostPacketProto.PayloadCase.LEAVE_UUID, leavePacket.getPayloadCase());
        assertEquals(uuid, leavePacket.getLeaveUuid());

        GhostPacketProto despawnPacket = ProtoConverter.decode(ProtoConverter.encodeDespawn(uuid));
        assertEquals(GhostPacketProto.PayloadCase.DESPAWN_UUID, despawnPacket.getPayloadCase());
        assertEquals(uuid, despawnPacket.getDespawnUuid());
    }

    @Test
    void authPackets_RoundTripAndProtocolVersion() throws Exception {
        // AUTH_CHALLENGE（デフォルトバージョン）
        GhostPacketProto challenge = ProtoConverter.decode(ProtoConverter.encodeAuthChallenge("nonce-abc"));
        assertEquals(GhostPacketProto.PayloadCase.AUTH_CHALLENGE, challenge.getPayloadCase());
        assertEquals("nonce-abc", challenge.getAuthChallenge().getNonce());
        assertEquals(ProtoConverter.PROTOCOL_VERSION, challenge.getAuthChallenge().getProtocolVersion());

        // AUTH_RESPONSE（明示バージョン指定）
        GhostPacketProto response = ProtoConverter.decode(ProtoConverter.encodeAuthResponse("hmac-xyz", 2));
        assertEquals(GhostPacketProto.PayloadCase.AUTH_RESPONSE, response.getPayloadCase());
        assertEquals("hmac-xyz", response.getAuthResponse().getHash());
        assertEquals(2, response.getAuthResponse().getProtocolVersion());

        // AUTH_SUCCESS
        GhostPacketProto success = ProtoConverter.decode(ProtoConverter.encodeAuthSuccess());
        assertEquals(GhostPacketProto.PayloadCase.AUTH_SUCCESS, success.getPayloadCase());
    }
}
