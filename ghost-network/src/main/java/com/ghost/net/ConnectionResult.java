package com.ghost.net;

import java.util.Objects;

/**
 * Ghost サーバーへの接続・認証試行の結果を表す不変レコード。
 *
 * @param connected     接続および CHAP 認証が完了した場合は {@code true}
 * @param failureReason 失敗時の理由（成功時は {@link FailureReason#NONE}）
 */
public record ConnectionResult(boolean connected, FailureReason failureReason) {

    public ConnectionResult {
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        if (connected && failureReason != FailureReason.NONE) {
            throw new IllegalArgumentException("Successful ConnectionResult must have FailureReason.NONE");
        }
        if (!connected && failureReason == FailureReason.NONE) {
            failureReason = FailureReason.CONNECTION_ERROR;
        }
    }

    /**
     * 接続成功を表す結果を返します。
     */
    public static ConnectionResult success() {
        return new ConnectionResult(true, FailureReason.NONE);
    }

    /**
     * 指定した失敗理由を持つ接続失敗結果を返します。
     */
    public static ConnectionResult failure(FailureReason reason) {
        return new ConnectionResult(false, Objects.requireNonNull(reason, "reason must not be null"));
    }

    /**
     * 接続失敗の原因分類。
     */
    public enum FailureReason {
        /** 成功（エラーなし） */
        NONE,
        /** 接続先 URL またはポートの設定値が不正 */
        INVALID_URI,
        /** サーバー未起動・到達不能・タイムアウト */
        UNREACHABLE,
        /** TLS ハンドシェイク失敗・証明書検証エラー */
        TLS_ERROR,
        /** パスワード不一致による認証または復号エラー */
        AUTH_FAILED,
        /** クライアントとサーバーのプロトコルバージョン不一致 */
        PROTOCOL_MISMATCH,
        /** その他の通信・プロトコルエラー */
        CONNECTION_ERROR
    }
}
