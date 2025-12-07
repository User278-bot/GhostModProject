package com.ghost.api.packet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GhostPacketTest {

    @Test
    void constructor_ShouldSetTypeAndData() {
        // 準備・実行
        String testData = "test-uuid";
        GhostPacket<String> packet = new GhostPacket<>(MessageType.LEAVE, testData);

        // 検証
        assertEquals(MessageType.LEAVE, packet.getType());
        assertEquals("test-uuid", packet.getData());
    }

    @Test
    void constructor_ShouldAllowNullData() {
        // 実行
        GhostPacket<String> packet = new GhostPacket<>(MessageType.UPDATE, null);

        // 検証
        assertEquals(MessageType.UPDATE, packet.getType());
        assertNull(packet.getData());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyPacket() {
        // 実行
        GhostPacket<Object> packet = new GhostPacket<>();

        // 検証
        assertNull(packet.getType());
        assertNull(packet.getData());
    }

    @Test
    void withIntegerData_ShouldWorkCorrectly() {
        // 準備・実行
        GhostPacket<Integer> packet = new GhostPacket<>(MessageType.JOIN, 42);

        // 検証
        assertEquals(MessageType.JOIN, packet.getType());
        assertEquals(42, packet.getData());
    }
}
