package com.ghost.util;

import com.ghost.util.WebSocketUriBuilder.UrlValidationError;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link WebSocketUriBuilder} のユニットテスト。
 */
class WebSocketUriBuilderTest {

    @Test
    void build_ExplicitWsAndWss_CaseInsensitive() {
        assertEquals("ws://localhost:8887", WebSocketUriBuilder.build("ws://localhost", 8887).toString());
        assertEquals("ws://localhost:8887", WebSocketUriBuilder.build("WS://localhost", 8887).toString());
        assertEquals("wss://localhost:8443", WebSocketUriBuilder.build("wss://localhost", 8443).toString());
        assertEquals("wss://localhost:8443", WebSocketUriBuilder.build("WSS://localhost", 8443).toString());
    }

    @Test
    void build_OmittedScheme_DefaultsToWs() {
        assertEquals("ws://localhost:8887", WebSocketUriBuilder.build("localhost", 8887).toString());
        assertEquals("ws://192.168.1.10:8887", WebSocketUriBuilder.build("  192.168.1.10  ", 8887).toString());
    }

    @Test
    void build_StandardPorts_OmitsPortNumber() {
        assertEquals("wss://ghost.example.com", WebSocketUriBuilder.build("wss://ghost.example.com", 443).toString());
        assertEquals("ws://ghost.example.com", WebSocketUriBuilder.build("ws://ghost.example.com", 80).toString());
        assertEquals("ws://ghost.example.com", WebSocketUriBuilder.build("ghost.example.com", 80).toString());
    }

    @Test
    void build_WithPath_PreservesPath() {
        assertEquals("wss://ghost.example.com/room1",
                WebSocketUriBuilder.build("wss://ghost.example.com/room1", 443).toString());
        assertEquals("ws://127.0.0.1:8887/ws",
                WebSocketUriBuilder.build("127.0.0.1/ws", 8887).toString());
    }

    @Test
    void build_Ipv6Literal_WrapsInBrackets() {
        assertEquals("ws://[::1]:8887", WebSocketUriBuilder.build("::1", 8887).toString());
        assertEquals("wss://[::1]:8443", WebSocketUriBuilder.build("wss://[::1]", 8443).toString());
        assertEquals("wss://[::1]/ws", WebSocketUriBuilder.build("wss://[::1]/ws", 443).toString());
    }

    @Test
    void validateUrlInput_ValidInputs_ReturnsEmpty() {
        assertEquals(Optional.empty(), WebSocketUriBuilder.validateUrlInput("ws://localhost"));
        assertEquals(Optional.empty(), WebSocketUriBuilder.validateUrlInput("WSS://ghost.example.com/sync"));
        assertEquals(Optional.empty(), WebSocketUriBuilder.validateUrlInput("localhost"));
        assertEquals(Optional.empty(), WebSocketUriBuilder.validateUrlInput("::1"));
    }

    @Test
    void validateUrlInput_InvalidInputs_ReturnsSpecificError() {
        assertEquals(Optional.of(UrlValidationError.EMPTY), WebSocketUriBuilder.validateUrlInput(null));
        assertEquals(Optional.of(UrlValidationError.EMPTY), WebSocketUriBuilder.validateUrlInput("   "));

        assertEquals(Optional.of(UrlValidationError.UNSUPPORTED_SCHEME),
                WebSocketUriBuilder.validateUrlInput("http://example.com"));
        assertEquals(Optional.of(UrlValidationError.UNSUPPORTED_SCHEME),
                WebSocketUriBuilder.validateUrlInput("https://example.com"));

        assertEquals(Optional.of(UrlValidationError.PORT_IN_URL),
                WebSocketUriBuilder.validateUrlInput("ws://localhost:8887"));
        assertEquals(Optional.of(UrlValidationError.PORT_IN_URL),
                WebSocketUriBuilder.validateUrlInput("localhost:8887"));
        assertEquals(Optional.of(UrlValidationError.PORT_IN_URL),
                WebSocketUriBuilder.validateUrlInput("wss://[::1]:8443"));

        assertEquals(Optional.of(UrlValidationError.INVALID_HOST),
                WebSocketUriBuilder.validateUrlInput("ws://"));
        assertEquals(Optional.of(UrlValidationError.INVALID_HOST),
                WebSocketUriBuilder.validateUrlInput("/only-path"));
    }

    @Test
    void build_InvalidInputs_ThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build(null, 8887));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("   ", 8887));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("ws://localhost", 0));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("ws://localhost", 65536));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("https://example.com", 443));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("ws://localhost:8887", 8887));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("/only-path", 443));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("wss://", 443));
    }
}
