package com.ghost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GhostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ghostmod.json");

    // 設定項目のデフォルト値を定義
    private String serverUri = "ws://localhost";
    private int serverPort = 8887;

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

    // 組み立てた完全なURIを取得するヘルパーメソッド
    public String getFullWebSocketUri() {
        return getServerUri() + ":" + getServerPort();
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

            LogUtils.getLogger().info("Configuration saved to: {}", CONFIG_FILE);
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
                this.serverPort = loaded.serverPort;
                this.serverUri = loaded.serverUri;
                LogUtils.getLogger().info("Configuration loaded from: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Failed to load configuration from file: {}", CONFIG_FILE, e);
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to parse configuration file (using defaults): {}", CONFIG_FILE, e);
        }
    }
}