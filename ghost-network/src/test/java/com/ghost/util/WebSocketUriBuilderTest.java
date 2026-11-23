package com.ghost.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link WebSocketUriBuilder} のユニットテスト。
 */
class WebSocketUriBuilderTest {

    @Test
    void build_PlainHostWithoutTls_AppendsWsAndPort() {
        URI uri = WebSocketUriBuilder.build("localhost", 8887, false);
        assertEquals("ws://localhost:8887", uri.toString());
    }

    @Test
    void build_PlainHostWithTls_AppendsWssAndOmitsDefaultPort443() {
        URI defaultPort = WebSocketUriBuilder.build("ghost.example.com", 443, true);
        assertEquals("wss://ghost.example.com", defaultPort.toString());

        URI customPort = WebSocketUriBuilder.build("ghost.example.com", 8443, true);
        assertEquals("wss://ghost.example.com:8443", customPort.toString());
    }

    @Test
    void build_PlainWsDefaultPort80_OmitsPort() {
        URI uri = WebSocketUriBuilder.build("ghost.example.com", 80, false);
        assertEquals("ws://ghost.example.com", uri.toString());
    }

    @Test
    void build_HostWithPath_PreservesPathAndAppliesScheme() {
        URI wssWithPath = WebSocketUriBuilder.build("ghost.example.com/room1", 443, true);
        assertEquals("wss://ghost.example.com/room1", wssWithPath.toString());

        URI wsCustomPortPath = WebSocketUriBuilder.build("127.0.0.1/ws", 8887, false);
        assertEquals("ws://127.0.0.1:8887/ws", wsCustomPortPath.toString());
    }

    @Test
    void build_ExplicitScheme_TakesPrecedenceOverToggle() {
        URI explicitWss = WebSocketUriBuilder.build("  wss://proxy.example.com/ghost  ", 8887, false);
        assertEquals("wss://proxy.example.com/ghost", explicitWss.toString());

        URI explicitWs = WebSocketUriBuilder.build("ws://localhost:9000", 443, true);
        assertEquals("ws://localhost:9000", explicitWs.toString());
    }

    @Test
    void build_Ipv6Literal_WrapsInBrackets() {
        URI ipv6 = WebSocketUriBuilder.build("::1", 8443, true);
        assertEquals("wss://[::1]:8443", ipv6.toString());

        URI bracketedIpv6 = WebSocketUriBuilder.build("[::1]/ws", 443, true);
        assertEquals("wss://[::1]/ws", bracketedIpv6.toString());
    }

    @Test
    void build_InvalidInputs_ThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build(null, 8887, false));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("   ", 8887, false));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("localhost", 0, false));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("localhost", 65536, false));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("https://example.com", 443, true));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("http://example.com", 80, false));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("/only-path", 443, true));
        assertThrows(IllegalArgumentException.class, () -> WebSocketUriBuilder.build("wss://", 443, true));
    }
}
