package com.ghost.config;

// 簡単のため、今はファイル保存機能は省略し、メモリ上のデータクラスとしてのみ機能させます。
public class GhostConfig {

    // 設定項目のデフォルト値を定義
    private String serverUri = "ws://localhost";
    private int serverPort = 8887;

    // --- シングルトンパターンでインスタンスを管理 ---
    private static final GhostConfig INSTANCE = new GhostConfig();

    public static GhostConfig getInstance() {
        return INSTANCE;
    }

    // --- ゲッターとセッター ---
    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String serverUri) {
        this.serverUri = serverUri;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    // 組み立てた完全なURIを取得するヘルパーメソッド
    public String getFullWebSocketUri() {
        return getServerUri() + ":" + getServerPort();
    }
}