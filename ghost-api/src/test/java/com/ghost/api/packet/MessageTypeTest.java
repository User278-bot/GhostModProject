package com.ghost.api.packet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void allValues_ShouldExist() {
        // 全てのMessageTypeが存在することを確認
        MessageType[] values = MessageType.values();

        assertEquals(5, values.length);
        assertNotNull(MessageType.UPDATE);
        assertNotNull(MessageType.INITIAL_SYNC);
        assertNotNull(MessageType.JOIN);
        assertNotNull(MessageType.LEAVE);
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
        assertEquals(1, MessageType.INITIAL_SYNC.ordinal());
        assertEquals(2, MessageType.JOIN.ordinal());
        assertEquals(3, MessageType.LEAVE.ordinal());
        assertEquals(4, MessageType.UNRECOGNIZED.ordinal());
    }
}
