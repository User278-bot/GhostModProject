package com.ghost.api.proto;

import com.ghost.api.dto.*;
import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.item.components.CustomModelDataDto;
import com.ghost.api.dto.item.components.TrimDto;
import com.ghost.api.packet.MessageType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtoConverterTest {

    @Test
    void testPlayerDataSerializationAndDeserialization() throws Exception {
        ItemDto head = new ItemDto.Builder("minecraft:diamond_helmet")
                .damage(10)
                .hasGlint(true)
                .color(0x00FF00)
                .trim(new TrimDto("coast", "diamond"))
                .customModelData(new CustomModelDataDto(List.of(1.0f, 2.5f), List.of(true), List.of("model1"), List.of(12345)))
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

        // 1. UPDATE パケットのシリアライズ
        byte[] serialized = ProtoConverter.serializeUpdatePacket(original);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        // 2. パケットタイプのデシリアライズ確認
        MessageType messageType = ProtoConverter.deserializePacketType(serialized);
        assertEquals(MessageType.UPDATE, messageType);

        // 3. PlayerData のデシリアライズ確認
        PlayerData deserialized = ProtoConverter.deserializeUpdatePacket(serialized);
        assertEquals(original, deserialized);
        assertEquals(original.pos(), deserialized.pos());
        assertEquals(original.rot(), deserialized.rot());
        assertEquals(original.uuid(), deserialized.uuid());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.equipment(), deserialized.equipment());
    }

    @Test
    void testUuidPacketSerializationAndDeserialization() throws Exception {
        String uuid = "abcdef12-3456-7890-abcd-ef1234567890";

        // LEAVE
        byte[] leaveBytes = ProtoConverter.serializeUuidPacket(MessageType.LEAVE, uuid);
        assertEquals(MessageType.LEAVE, ProtoConverter.deserializePacketType(leaveBytes));
        assertEquals(uuid, ProtoConverter.deserializeUuid(leaveBytes));

        // DESPAWN
        byte[] despawnBytes = ProtoConverter.serializeUuidPacket(MessageType.DESPAWN, uuid);
        assertEquals(MessageType.DESPAWN, ProtoConverter.deserializePacketType(despawnBytes));
        assertEquals(uuid, ProtoConverter.deserializeUuid(despawnBytes));
    }
}
