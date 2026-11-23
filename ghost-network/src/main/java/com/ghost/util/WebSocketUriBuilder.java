package com.ghost.util;

import java.net.URI;
import java.util.Locale;

/**
 * ホスト名／URI・ポート・TLS 設定から WebSocket 接続用 {@link URI} を安全に構築するユーティリティ。
 *
 * <ul>
 *   <li>入力に {@code ws://} または {@code wss://} が明示されている場合は、そのスキーム・ポート・パスを優先します。</li>
 *   <li>スキーム省略時は {@code useTls} に応じて {@code wss://} または {@code ws://} を付与し、
 *       パス付きホスト（例: {@code example.com/ghost}）や IPv6 リテラルにも対応します。</li>
 *   <li>標準ポート（{@code wss} の 443、{@code ws} の 80）ではポート番号を省略し、
 *       Nginx 等のリバースプロキシにおける {@code Host} ヘッダ一致・SNI との親和性を高めます。</li>
 * </ul>
 */
public final class WebSocketUriBuilder {

    private static final int DEFAULT_WS_PORT = 80;
    private static final int DEFAULT_WSS_PORT = 443;

    private WebSocketUriBuilder() {
    }

    /**
     * WebSocket 接続用 URI を構築します。
     *
     * @param rawHostOrUri ホスト名、パス付きホスト、または {@code ws://} / {@code wss://} 付き URI
     * @param port         ポート番号（1〜65535）
     * @param useTls       スキーム省略時に {@code wss://} を使用する場合は true
     * @return 検証済みの {@link URI}
     * @throws IllegalArgumentException 入力が空・不正スキーム・不正ポート・不正 URI の場合
     */
    public static URI build(String rawHostOrUri, int port, boolean useTls) {
        if (rawHostOrUri == null || rawHostOrUri.isBlank()) {
            throw new IllegalArgumentException("Server URI must not be null or blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535: " + port);
        }

        String trimmed = rawHostOrUri.trim();

        // 明示スキームが含まれる場合は入力をそのまま検証して尊重する
        if (trimmed.contains("://")) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("ws://") && !lower.startsWith("wss://")) {
                throw new IllegalArgumentException("Unsupported WebSocket scheme: " + trimmed);
            }
            URI explicitUri = URI.create(trimmed);
            validateHost(explicitUri, trimmed);
            return explicitUri;
        }

        // ホスト部とパス部を分離（例: "example.com/ghost" -> "example.com", "/ghost"）
        int slashIndex = trimmed.indexOf('/');
        String hostPart = slashIndex >= 0 ? trimmed.substring(0, slashIndex) : trimmed;
        String pathPart = slashIndex >= 0 ? trimmed.substring(slashIndex) : "";
        if (hostPart.isEmpty()) {
            throw new IllegalArgumentException("Host part must not be empty: " + trimmed);
        }

        String formattedHost = formatHostForUri(hostPart);
        String scheme = useTls ? "wss" : "ws";
        boolean omitPort = (useTls && port == DEFAULT_WSS_PORT) || (!useTls && port == DEFAULT_WS_PORT);

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(formattedHost);
        if (!omitPort) {
            sb.append(':').append(port);
        }
        sb.append(pathPart);

        URI built = URI.create(sb.toString());
        validateHost(built, trimmed);
        return built;
    }

    private static String formatHostForUri(String hostPart) {
        // 裸の IPv6 アドレス（コロンが 2 つ以上あり、角括弧未付与）は URI 形式に合わせ角括弧で囲む
        if (!hostPart.startsWith("[") && countColons(hostPart) >= 2) {
            return "[" + hostPart + "]";
        }
        return hostPart;
    }

    private static int countColons(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ':') {
                count++;
            }
        }
        return count;
    }

    private static void validateHost(URI uri, String originalInput) {
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Invalid WebSocket host in URI: " + originalInput);
        }
    }
}
