package com.ghost.server;

import com.ghost.common.dto.PlayerData;
import com.ghost.net.packet.GhostPacket;
import com.ghost.net.packet.MessageType;
import com.ghost.util.SerializationUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.*;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GhostModServer extends WebSocketServer {

    // 接続中のクライアントをスレッドセーフに管理するためのセット
    private static final Map<WebSocket, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostModServer.class);

    public GhostModServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onStart() {
        // サーバーが起動したときの処理
        LOGGER.info("Server started on port: {}", getPort());
        setConnectionLostTimeout(100); // 接続タイムアウトの設定（秒）
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // 新しいクライアントが接続したときの処理
        LOGGER.info("New player connected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // クライアントが切断したときの処理
        var disconnectedPlayer = playerDataMap.remove(conn);
        if (disconnectedPlayer != null) {
            LOGGER.info("Player {} disconnected: {} (Code: {}, Reason: {}, Remote: {})", disconnectedPlayer.name(),
                    conn.getRemoteSocketAddress(), code, reason, remote);
            sendLeavePacket(disconnectedPlayer);
        } else {
            LOGGER.warn("Disconnected a client that had not sent any data: {}", conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // クライアントからメッセージ（プレイヤー情報など）を受信したときの処理
        LOGGER.info("Message from {}: {}", conn.getRemoteSocketAddress(), message);

        var packet = SerializationUtil.deserializePacket(message);
        if (packet == null || packet.getType() != MessageType.UPDATE) {
            LOGGER.warn("Received invalid packet from {}", conn.getRemoteSocketAddress());
            return;
        }
        var playerData = SerializationUtil.parsePlayerData(packet.getData());
        if (playerData == null) {
            LOGGER.warn("Failed to parse PlayerData from {}", conn.getRemoteSocketAddress());
            return;
        }
        var previous = playerDataMap.put(conn, playerData);
        if (previous == null) {
            sendInitialPaket(conn);
            var joinedPlayer = SerializationUtil.parsePlayerData(packet.getData());
            sendJoinPacket(conn, joinedPlayer);
        } else {
            // ★重要：受信したメッセージを、送信元以外の全クライアントに転送（ブロードキャスト）する
            broadcast(message, conn);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // エラーが発生したときの処理
        // ex.toString() の代わりにexを直接渡すことで、スタックトレースがログに出力され、デバッグが容易になります。
        if (conn != null) {
            LOGGER.error("An error occurred on connection {}:", conn.getRemoteSocketAddress(), ex);
            playerDataMap.remove(conn);
        } else {
            LOGGER.error("An error occurred:", ex);
        }
    }

    /**
     * 特定のクライアントを除いて、接続中の全クライアントにメッセージを送信する
     *
     * @param message 送信するメッセージ
     * @param exclude 除外するクライアント
     */
    public void broadcast(String message, WebSocket exclude) {
        // スレッドセーフなSetを安全にループするため、synchronizedブロックで囲みます
        var connections = this.getConnections();
        synchronized (connections) {
            for (WebSocket conn : connections) {
                if (conn != null && conn.isOpen() && !conn.equals(exclude)) {
                    conn.send(message);
                }
            }
        }
    }

    private void sendInitialPaket(WebSocket newConnection) {
        var excludePlayers = playerDataMap.entrySet().stream()
                .filter(
                        (entry) -> !entry.getKey().equals(newConnection))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        if (excludePlayers.isEmpty()) {
            return;
        }

        var packet = new GhostPacket<>(MessageType.INITIAL_SYNC, excludePlayers);
        var msg = SerializationUtil.serializePacket(packet);
        newConnection.send(msg);
    }

    private void sendLeavePacket(final PlayerData playerData) {
        var leavePacket = new GhostPacket<>(MessageType.LEAVE, playerData.uuid());
        var msg = SerializationUtil.serializePacket(leavePacket);
        broadcast(msg);
    }

    private void sendJoinPacket(WebSocket conn, PlayerData joinedPlayer) {
        var joinPacket = new GhostPacket<>(MessageType.JOIN, joinedPlayer);
        var msg = SerializationUtil.serializePacket(joinPacket);
        broadcast(msg, conn);
        LOGGER.info("Player '{}' joined.", joinedPlayer.name());
    }

    public static void main(String[] args) {
        int port = 8887; // デフォルトポート

        // --- コマンドライン引数の定義 ---
        Options options = new Options();
        options.addOption(Option.builder().longOpt("port").hasArg().desc("Server Port").build());

        // --- 引数の解析 ---
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("port")) {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            }
        } catch (ParseException e) {
            LOGGER.error("Failed to parse command line arguments: {}", e.getMessage());
            return;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid port number format.");
            return;
        }

        try {
            GhostModServer server = new GhostModServer(port);
            server.start(); // サーバーを起動
        } catch (Exception ex) {
            LOGGER.error("Failed to start WebSocket server", ex);
        }
    }
}