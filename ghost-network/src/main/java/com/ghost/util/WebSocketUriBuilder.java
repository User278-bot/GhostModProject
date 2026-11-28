package com.ghost.util;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * サーバー URL 入力値とポート番号から WebSocket 接続用 {@link URI} を安全に構築・検証するユーティリティ。
 *
 * <ul>
 *   <li>入力に {@code ws://} または {@code wss://}（大文字・小文字非区別）が指定されている場合はそのスキームを採用します。</li>
 *   <li>プロトコル（スキーム）が省略されている場合は {@code ws://} として解釈します。</li>
 *   <li>ポート番号は別項目で指定する前提とし、URL 文字列内にポートが含まれている場合はエラーとして検出します。</li>
 *   <li>標準ポート（{@code wss} の 443、{@code ws} の 80）では URI 上のポート表記を省略し、
 *       Nginx 等のリバースプロキシにおける {@code Host} ヘッダ一致・SNI との親和性を高めます。</li>
 * </ul>
 */
public final class WebSocketUriBuilder {

    private static final int DEFAULT_WS_PORT = 80;
    private static final int DEFAULT_WSS_PORT = 443;

    /**
     * URL 入力欄のバリデーションエラー種別。
     */
    public enum UrlValidationError {
        /** 未入力または空白のみ */
        EMPTY,
        /** {@code ws://} / {@code wss://} 以外の非対応スキーム（{@code http://} 等） */
        UNSUPPORTED_SCHEME,
        /** URL 文字列内にポート番号が含まれている（ポート欄との二重指定） */
        PORT_IN_URL,
        /** ホスト名が空または URI として不正 */
        INVALID_HOST
    }

    private record ParsedUrlParts(String scheme, String formattedHost, String pathPart) {
    }

    private static final class UrlValidationException extends IllegalArgumentException {
        private final UrlValidationError error;

        private UrlValidationException(UrlValidationError error, String message) {
            super(message);
            this.error = error;
        }

        private UrlValidationException(UrlValidationError error, String message, Throwable cause) {
            super(message, cause);
            this.error = error;
        }

        private UrlValidationError error() {
            return error;
        }
    }

    private WebSocketUriBuilder() {
    }

    /**
     * URL 入力欄の文字列のみを検証し、問題がある場合はエラー種別を返します。
     *
     * @param rawUrl ユーザーが入力したサーバー URL 文字列
     * @return 正常な場合は {@link Optional#empty()}、不正な場合は {@link UrlValidationError}
     */
    public static Optional<UrlValidationError> validateUrlInput(String rawUrl) {
        try {
            parseAndBuild(rawUrl, 8887);
            return Optional.empty();
        } catch (UrlValidationException ex) {
            return Optional.of(ex.error());
        } catch (IllegalArgumentException ex) {
            return Optional.of(UrlValidationError.INVALID_HOST);
        }
    }

    /**
     * サーバー URL 文字列とポート番号から WebSocket 接続用 {@link URI} を構築します。
     *
     * @param rawUrl {@code ws://} / {@code wss://} 付き URL、またはスキーム省略ホスト（パス付き・IPv6 可）
     * @param port   ポート番号（1〜65535）
     * @return 検証済みの {@link URI}
     * @throws IllegalArgumentException 入力が空・不正スキーム・URL 内ポート指定・不正ポート・不正ホストの場合
     */
    public static URI build(String rawUrl, int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535: " + port);
        }
        return parseAndBuild(rawUrl, port);
    }

    private static URI parseAndBuild(String rawUrl, int port) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new UrlValidationException(UrlValidationError.EMPTY, "Server URL must not be null or blank");
        }

        String trimmed = rawUrl.trim();
        ParsedUrlParts parts = parseUrlParts(trimmed);

        boolean omitPort = ("wss".equals(parts.scheme()) && port == DEFAULT_WSS_PORT)
                || ("ws".equals(parts.scheme()) && port == DEFAULT_WS_PORT);

        StringBuilder sb = new StringBuilder();
        sb.append(parts.scheme()).append("://").append(parts.formattedHost());
        if (!omitPort) {
            sb.append(':').append(port);
        }
        sb.append(parts.pathPart());

        final URI built;
        try {
            built = URI.create(sb.toString());
        } catch (IllegalArgumentException ex) {
            throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Invalid WebSocket URI: " + trimmed, ex);
        }

        if (built.getHost() == null || built.getHost().isBlank() || built.getUserInfo() != null) {
            throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Invalid WebSocket host in URI: " + trimmed);
        }
        return built;
    }

    private static ParsedUrlParts parseUrlParts(String trimmed) {
        String scheme;
        String remainder;

        int schemeSep = trimmed.indexOf("://");
        if (schemeSep >= 0) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("ws://")) {
                scheme = "ws";
                remainder = trimmed.substring(5);
            } else if (lower.startsWith("wss://")) {
                scheme = "wss";
                remainder = trimmed.substring(6);
            } else {
                throw new UrlValidationException(
                        UrlValidationError.UNSUPPORTED_SCHEME,
                        "Unsupported WebSocket scheme (expected ws:// or wss://): " + trimmed);
            }
        } else {
            // プロトコル省略時は ws:// として解釈する
            scheme = "ws";
            remainder = trimmed;
        }

        if (remainder.isEmpty()) {
            throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Host part must not be empty: " + trimmed);
        }

        int slashIndex = remainder.indexOf('/');
        String hostPart = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
        String pathPart = slashIndex >= 0 ? remainder.substring(slashIndex) : "";
        if (hostPart.isEmpty()) {
            throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Host part must not be empty: " + trimmed);
        }

        String formattedHost = validateAndFormatHost(hostPart, trimmed);
        return new ParsedUrlParts(scheme, formattedHost, pathPart);
    }

    private static String validateAndFormatHost(String hostPart, String originalInput) {
        if (hostPart.startsWith("[")) {
            int closeBracket = hostPart.indexOf(']');
            if (closeBracket <= 1) {
                throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Invalid IPv6 host: " + originalInput);
            }
            if (closeBracket < hostPart.length() - 1) {
                if (hostPart.charAt(closeBracket + 1) == ':') {
                    throw new UrlValidationException(
                            UrlValidationError.PORT_IN_URL,
                            "Port must be specified in the separate port field, not in URL: " + originalInput);
                }
                throw new UrlValidationException(UrlValidationError.INVALID_HOST, "Invalid characters after IPv6 host: " + originalInput);
            }
            return hostPart;
        }

        int colons = countColons(hostPart);
        if (colons == 1) {
            throw new UrlValidationException(
                    UrlValidationError.PORT_IN_URL,
                    "Port must be specified in the separate port field, not in URL: " + originalInput);
        }
        if (colons >= 2) {
            // 裸の IPv6 リテラルは角括弧で囲む
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
}
