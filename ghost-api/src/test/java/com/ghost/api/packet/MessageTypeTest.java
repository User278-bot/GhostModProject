package com.ghost.api.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// このテスト不要説があるけど、一応残しておく

class MessageTypeTest {

    @Test
    void allValues_ShouldExist() {
        // 全てのMessageTypeが存在することを確認
        MessageType[] values = MessageType.values();

        assertEquals(8, values.length);
        assertNotNull(MessageType.UPDATE);
        assertNotNull(MessageType.JOIN);
        assertNotNull(MessageType.LEAVE);
        assertNotNull(MessageType.DESPAWN);
        assertNotNull(MessageType.AUTH_CHALLENGE);
        assertNotNull(MessageType.AUTH_RESPONSE);
        assertNotNull(MessageType.AUTH_SUCCESS);
        assertNotNull(MessageType.UNRECOGNIZED);
    }

    @Test
    void valueOf_ShouldReturnCorrectValue() {
        assertEquals(MessageType.UPDATE, MessageType.valueOf("UPDATE"));
        assertEquals(MessageType.LEAVE, MessageType.valueOf("LEAVE"));
    }

    @Test
    void valueOf_InvalidName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageType.valueOf("INVALID_TYPE");
        });
    }

    @Test
    void ordinal_ShouldBeConsistent() {
        assertEquals(0, MessageType.UPDATE.ordinal());
        assertEquals(1, MessageType.JOIN.ordinal());
        assertEquals(2, MessageType.LEAVE.ordinal());
        assertEquals(3, MessageType.DESPAWN.ordinal());
        assertEquals(4, MessageType.AUTH_CHALLENGE.ordinal());
        assertEquals(5, MessageType.AUTH_RESPONSE.ordinal());
        assertEquals(6, MessageType.AUTH_SUCCESS.ordinal());
        assertEquals(7, MessageType.UNRECOGNIZED.ordinal());
    }
}
