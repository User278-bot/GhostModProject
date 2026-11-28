package com.ghost.server;

import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 信頼済みリバースプロキシ経由の接続から実クライアント IP アドレスを安全に解決するユーティリティ。
 * <p>
 * ソケットの直接接続元（peer）が信頼済みプロキシ（単一 IP または IPv4/IPv6 CIDR）に一致する場合のみ
 * {@code X-Forwarded-For} / {@code X-Real-IP} を解釈します。
 * {@code X-Forwarded-For} はクライアントによる先頭偽装を防ぐため右端から左へ走査し、
 * 最初に見つかった「信頼済みプロキシではない有効な IP」を採用します。
 * また、DNS ルックアップによるブロッキングや情報漏洩を防ぐため、数値 IP リテラルのみを厳密にパースします。
 */
public final class ClientIpResolver {

    private final List<SubnetMask> trustedProxies;

    public ClientIpResolver(@Nullable List<String> trustedProxySpecs) {
        this.trustedProxies = parseAllSpecs(trustedProxySpecs);
    }

    /**
     * 信頼済みプロキシ指定リスト（単一 IP または CIDR）の妥当性を検証します。
     * 不正な書式が含まれる場合は {@link IllegalArgumentException} を送出します。
     */
    public static void validateSpecs(@Nullable List<String> trustedProxySpecs) {
        parseAllSpecs(trustedProxySpecs);
    }

    /**
     * 信頼済みプロキシが 1 件以上登録されているかどうかを返します。
     */
    public boolean hasTrustedProxies() {
        return !trustedProxies.isEmpty();
    }

    /**
     * 指定されたアドレスが信頼済みプロキシに一致するかを判定します。
     */
    public boolean isTrustedProxy(@Nullable InetAddress address) {
        if (address == null || trustedProxies.isEmpty()) {
            return false;
        }
        for (SubnetMask mask : trustedProxies) {
            if (mask.matches(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 接続元アドレスとプロキシ転送ヘッダから実クライアント IP を解決します。
     * 接続元が信頼済みプロキシに一致しない場合はヘッダを完全に無視し {@code peerAddress} を返します。
     */
    public InetAddress resolve(InetAddress peerAddress,
                               @Nullable String xForwardedFor,
                               @Nullable String xRealIp) {
        Objects.requireNonNull(peerAddress, "peerAddress must not be null");

        if (!isTrustedProxy(peerAddress)) {
            return peerAddress;
        }

        InetAddress fromXff = resolveFromXForwardedFor(xForwardedFor);
        if (fromXff != null) {
            return fromXff;
        }

        InetAddress fromRealIp = parseIpLiteral(xRealIp);
        if (fromRealIp != null) {
            return fromRealIp;
        }

        return peerAddress;
    }

    @Nullable
    private InetAddress resolveFromXForwardedFor(@Nullable String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return null;
        }
        String[] parts = xForwardedFor.split(",", -1);
        InetAddress leftmostTrusted = null;

        // クライアントが偽装できる左端ではなく、直近プロキシが追記した右端から左へ走査する
        for (int i = parts.length - 1; i >= 0; i--) {
            InetAddress candidate = parseIpLiteral(parts[i]);
            if (candidate == null) {
                // 不正なトークンより左は信頼できないため走査を打ち切る
                break;
            }
            if (!isTrustedProxy(candidate)) {
                return candidate;
            }
            leftmostTrusted = candidate;
        }
        return leftmostTrusted;
    }

    private static List<SubnetMask> parseAllSpecs(@Nullable List<String> specs) {
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        List<SubnetMask> parsed = new ArrayList<>(specs.size());
        for (String spec : specs) {
            parsed.add(parseSpec(spec));
        }
        return List.copyOf(parsed);
    }

    private static SubnetMask parseSpec(@Nullable String rawSpec) {
        if (rawSpec == null || rawSpec.isBlank()) {
            throw new IllegalArgumentException("Trusted proxy entry must not be blank");
        }
        String trimmed = rawSpec.trim();
        int slashIdx = trimmed.indexOf('/');
        String ipPart;
        String prefixPart;
        if (slashIdx >= 0) {
            if (trimmed.indexOf('/', slashIdx + 1) >= 0) {
                throw new IllegalArgumentException("Invalid CIDR specification: " + rawSpec);
            }
            ipPart = trimmed.substring(0, slashIdx).trim();
            prefixPart = trimmed.substring(slashIdx + 1).trim();
        } else {
            ipPart = trimmed;
            prefixPart = null;
        }

        InetAddress address = parseIpLiteral(ipPart);
        if (address == null) {
            throw new IllegalArgumentException("Invalid IP address in trusted proxy specification: " + rawSpec);
        }

        byte[] bytes = address.getAddress();
        int maxPrefix = bytes.length * 8;
        int prefixLength;
        if (prefixPart == null) {
            prefixLength = maxPrefix;
        } else {
            if (prefixPart.isEmpty() || hasNonAsciiDigit(prefixPart)) {
                throw new IllegalArgumentException("Invalid CIDR prefix length: " + rawSpec);
            }
            try {
                prefixLength = Integer.parseInt(prefixPart);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid CIDR prefix length: " + rawSpec, e);
            }
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                throw new IllegalArgumentException(
                        "CIDR prefix length out of range (0.." + maxPrefix + "): " + rawSpec);
            }
        }

        return new SubnetMask(bytes, prefixLength);
    }

    /**
     * DNS 解決を一切行わず、数値 IPv4 / IPv6 リテラルのみを {@link InetAddress} へ変換します。
     */
    @Nullable
    static InetAddress parseIpLiteral(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        // 角括弧付き IPv6（例: [::1]）に対応
        if (s.startsWith("[") && s.endsWith("]") && s.length() > 2) {
            s = s.substring(1, s.length() - 1);
        }

        byte[] bytes = s.indexOf(':') >= 0 ? parseIpv6Bytes(s) : parseIpv4Bytes(s);
        if (bytes == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static byte @Nullable [] parseIpv4Bytes(String s) {
        String[] octets = s.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            String part = octets[i];
            if (part.isEmpty() || part.length() > 3 || hasNonAsciiDigit(part)) {
                return null;
            }
            if (part.length() > 1 && part.charAt(0) == '0') {
                return null;
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                return null;
            }
            result[i] = (byte) value;
        }
        return result;
    }

    private static byte @Nullable [] parseIpv6Bytes(String s) {
        // ゾーン ID 付き（fe80::1%eth0）はサポート対象外
        if (s.indexOf('%') >= 0) {
            return null;
        }

        int doubleColonIdx = s.indexOf("::");
        if (doubleColonIdx >= 0 && s.indexOf("::", doubleColonIdx + 2) >= 0) {
            return null;
        }

        // 末尾 IPv4 埋め込み形式（例: ::ffff:127.0.0.1）の検出と変換
        byte[] embeddedIpv4 = null;
        int lastColon = s.lastIndexOf(':');
        if (lastColon >= 0 && s.indexOf('.', lastColon + 1) >= 0) {
            String ipv4Part = s.substring(lastColon + 1);
            embeddedIpv4 = parseIpv4Bytes(ipv4Part);
            if (embeddedIpv4 == null) {
                return null;
            }
            // "::ffff:127.0.0.1" の "::" 直後に IPv4 があるケース（例: "::127.0.0.1"）を壊さないよう調整
            if (doubleColonIdx >= 0 && lastColon == doubleColonIdx + 1) {
                s = s.substring(0, doubleColonIdx + 2);
            } else {
                s = s.substring(0, lastColon);
            }
        }

        int maxGroups = embeddedIpv4 != null ? 6 : 8;
        int[] headGroups;
        int[] tailGroups;

        if (doubleColonIdx >= 0) {
            String head = s.substring(0, doubleColonIdx);
            String tail = s.substring(doubleColonIdx + 2);
            headGroups = head.isEmpty() ? new int[0] : parseIpv6Groups(head);
            tailGroups = tail.isEmpty() ? new int[0] : parseIpv6Groups(tail);
            if (headGroups == null || tailGroups == null) {
                return null;
            }
            if (headGroups.length + tailGroups.length >= maxGroups) {
                return null;
            }
        } else {
            headGroups = parseIpv6Groups(s);
            tailGroups = new int[0];
            if (headGroups == null || headGroups.length != maxGroups) {
                return null;
            }
        }

        byte[] result = new byte[16];
        int offset = 0;
        for (int g : headGroups) {
            result[offset++] = (byte) ((g >>> 8) & 0xFF);
            result[offset++] = (byte) (g & 0xFF);
        }
        int zeroGroups = maxGroups - headGroups.length - tailGroups.length;
        offset += zeroGroups * 2;
        for (int g : tailGroups) {
            result[offset++] = (byte) ((g >>> 8) & 0xFF);
            result[offset++] = (byte) (g & 0xFF);
        }
        if (embeddedIpv4 != null) {
            System.arraycopy(embeddedIpv4, 0, result, 12, 4);
        }
        return result;
    }

    private static int @Nullable [] parseIpv6Groups(String section) {
        String[] parts = section.split(":", -1);
        int[] groups = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 4) {
                return null;
            }
            int value = 0;
            for (int j = 0; j < part.length(); j++) {
                int digit = Character.digit(part.charAt(j), 16);
                if (digit < 0) {
                    return null;
                }
                value = (value << 4) | digit;
            }
            groups[i] = value;
        }
        return groups;
    }

    private static boolean hasNonAsciiDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return true;
            }
        }
        return false;
    }

    private record SubnetMask(byte[] networkBytes, int prefixLength) {

        boolean matches(InetAddress address) {
            byte[] addrBytes = address.getAddress();
            if (addrBytes.length != networkBytes.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addrBytes[i] != networkBytes[i]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % 8;
            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }
            return true;
        }
    }
}
