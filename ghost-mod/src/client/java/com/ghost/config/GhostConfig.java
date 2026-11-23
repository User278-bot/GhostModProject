package com.ghost.config;

import com.ghost.util.WebSocketUriBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class GhostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ghostmod.json");

    // 設定項目のデフォルト値を定義
    private String serverUri = "localhost";
    private int serverPort = 8887;
    private boolean useTls = false;
    private String serverPassword = "changeme";

    // --- シングルトンパターンでインスタンスを管理 ---
    private static final GhostConfig INSTANCE = new GhostConfig();

    static {
        // クラスロード時に設定を読み込む
        INSTANCE.load();
    }

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

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    /**
     * 現在の設定から WebSocket 接続用の {@link URI} を構築します。
     */
    public URI toWebSocketUri() {
        return WebSocketUriBuilder.build(getServerUri(), getServerPort(), isUseTls());
    }

    // --- ファイル永続化メソッド ---

    /**
     * 現在の設定をJSONファイルに保存します
     */
    public void save() {
        try {
            // 設定ディレクトリが存在しない場合は作成
            Files.createDirectories(CONFIG_FILE.getParent());

            // 設定をJSON形式でファイルに書き出し
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_FILE, json);

            LogUtils.getLogger().debug("Configuration saved to: {}", CONFIG_FILE);
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to save configuration to file: {}", CONFIG_FILE, e);
        }
    }

    /**
     * JSONファイルから設定を読み込みます。
     * ファイルが存在しない場合はデフォルト値を使用します。
     */
    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            LogUtils.getLogger().info("Configuration file not found. Using default values: {}", CONFIG_FILE);
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            GhostConfig loaded = GSON.fromJson(json, GhostConfig.class);

            // 読み込んだ値を現在のインスタンスにコピー
            if (loaded != null) {
                restoreFrom(loaded);
                LogUtils.getLogger().debug("Configuration loaded from: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load configuration from file: {}", CONFIG_FILE, e);
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to parse configuration file (using defaults): {}", CONFIG_FILE, e);
        }
    }

    private void restoreFrom(GhostConfig config) {
        this.serverPort = (config.serverPort >= 1 && config.serverPort <= 65535) ? config.serverPort : 8887;
        this.serverUri = (config.serverUri != null && !config.serverUri.isBlank()) ? config.serverUri : "localhost";
        this.useTls = config.useTls;
        this.serverPassword = config.serverPassword != null ? config.serverPassword : "changeme";
    }

    /**
     * 設定値を保持する不変のスナップショット。
     * 編集中の設定値を一時的に保持し、接続テストなどに使用します。
     */
    public record ConfigSnapshot(String serverUri, int serverPort, boolean useTls, String serverPassword) {
        /**
         * WebSocket 接続用の {@link URI} を構築します。
         */
        public URI toWebSocketUri() {
            return WebSocketUriBuilder.build(serverUri, serverPort, useTls);
        }
    }
}