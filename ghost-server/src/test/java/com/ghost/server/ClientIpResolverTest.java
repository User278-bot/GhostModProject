package com.ghost.server;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ClientIpResolver} の信頼済みプロキシ判定・X-Forwarded-For 右端走査・フォールバック・バリデーションのテスト。
 */
class ClientIpResolverTest {

    @Test
    void resolve_IgnoresHeadersWhenNoTrustedProxiesConfigured() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of());
        InetAddress peer = InetAddress.getByName("198.51.100.20");

        InetAddress resolved = resolver.resolve(peer, "203.0.113.10", "203.0.113.11");

        assertFalse(resolver.hasTrustedProxies());
        assertEquals(peer, resolved);
    }

    @Test
    void resolve_IgnoresHeadersFromUntrustedPeer() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1", "10.0.0.0/8"));
        InetAddress untrustedPeer = InetAddress.getByName("198.51.100.99");

        InetAddress resolved = resolver.resolve(untrustedPeer, "203.0.113.10", "203.0.113.11");

        assertTrue(resolver.hasTrustedProxies());
        assertFalse(resolver.isTrustedProxy(untrustedPeer));
        assertEquals(untrustedPeer, resolved);
    }

    @Test
    void resolve_ExtractsClientIpFromSingleTrustedProxy() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1"));
        InetAddress peer = InetAddress.getByName("127.0.0.1");

        InetAddress resolved = resolver.resolve(peer, "203.0.113.10", null);

        assertEquals(InetAddress.getByName("203.0.113.10"), resolved);
    }

    @Test
    void resolve_ScansXForwardedForRightToLeftAndIgnoresSpoofedLeftIp() throws Exception {
        // クライアントが左端に偽装 IP (1.2.3.4) を送り、CDN (10.0.0.5) → Nginx (127.0.0.1) を経由した想定
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1", "10.0.0.0/8"));
        InetAddress peer = InetAddress.getByName("127.0.0.1");

        InetAddress resolved = resolver.resolve(
                peer, "1.2.3.4, 203.0.113.42, 10.0.0.5", "192.0.2.1");

        assertEquals(InetAddress.getByName("203.0.113.42"), resolved);
    }

    @Test
    void resolve_ReturnsLeftmostTrustedWhenAllHopsAreTrusted() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1", "10.0.0.0/8"));
        InetAddress peer = InetAddress.getByName("127.0.0.1");

        InetAddress resolved = resolver.resolve(peer, "10.0.0.2, 10.0.0.5", null);

        assertEquals(InetAddress.getByName("10.0.0.2"), resolved);
    }

    @Test
    void resolve_SupportsIpv6SingleAndCidr() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("::1", "fd00::/8"));
        InetAddress peer = InetAddress.getByName("::1");

        InetAddress resolved = resolver.resolve(
                peer, "2001:db8::cafe, fd12:3456::1", null);

        assertEquals(InetAddress.getByName("2001:db8::cafe"), resolved);
    }

    @Test
    void resolve_FallsBackToXRealIpWhenXForwardedForIsEmptyOrMalformed() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1"));
        InetAddress peer = InetAddress.getByName("127.0.0.1");

        // X-Forwarded-For が空文字（Java-WebSocket のヘッダ未存在時の戻り値）
        assertEquals(InetAddress.getByName("203.0.113.77"),
                resolver.resolve(peer, "", "203.0.113.77"));

        // X-Forwarded-For の右端が不正値（ホスト名等・DNS 解決しないこと）
        assertEquals(InetAddress.getByName("203.0.113.88"),
                resolver.resolve(peer, "not-an-ip.invalid", "203.0.113.88"));
    }

    @Test
    void resolve_FallsBackToPeerAddressWhenAllHeadersInvalid() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1"));
        InetAddress peer = InetAddress.getByName("127.0.0.1");

        assertEquals(peer, resolver.resolve(peer, "invalid-ip", "also-invalid"));
        assertEquals(peer, resolver.resolve(peer, null, null));
    }

    @Test
    void resolve_CidrBoundaryMatchingIsAccurate() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(List.of("172.16.0.0/12"));

        assertTrue(resolver.isTrustedProxy(InetAddress.getByName("172.16.0.1")));
        assertTrue(resolver.isTrustedProxy(InetAddress.getByName("172.31.255.254")));
        assertFalse(resolver.isTrustedProxy(InetAddress.getByName("172.15.255.255")));
        assertFalse(resolver.isTrustedProxy(InetAddress.getByName("172.32.0.1")));
    }

    @Test
    void validateSpecs_RejectsInvalidIpAndCidr() {
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("example.com")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("127.0.0.1/33")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("127.0.0.1/-1")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("::1/129")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("10.0.0.0/8/16")));
        assertThrows(IllegalArgumentException.class, () -> ClientIpResolver.validateSpecs(List.of("999.0.0.1")));
    }
}
