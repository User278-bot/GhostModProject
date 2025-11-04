package com.ghost.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GhostModServer extends WebSocketServer {

    // 接続中のクライアントをスレッドセーフに管理するためのセット
    private static final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());
    private static final Logger logger = LoggerFactory.getLogger(GhostModServer.class);

    public GhostModServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onStart() {
        // サーバーが起動したときの処理
        logger.info("Server started on port: {}", getPort());
        setConnectionLostTimeout(100); // 接続タイムアウトの設定（秒）
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // 新しいクライアントが接続したときの処理
        connections.add(conn);
        logger.info("New player connected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // クライアントが切断したときの処理
        connections.remove(conn);
        logger.info("Player disconnected: {} (Code: {}, Reason: {}, Remote: {})", conn.getRemoteSocketAddress(), code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // クライアントからメッセージ（プレイヤー情報など）を受信したときの処理
        logger.info("Message from {}: {}", conn.getRemoteSocketAddress(), message);

        // ★重要：受信したメッセージを、送信元以外の全クライアントに転送（ブロードキャスト）する
        broadcast(message, conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // エラーが発生したときの処理
        // ex.toString() の代わりにexを直接渡すことで、スタックトレースがログに出力され、デバッグが容易になります。
        if (conn != null) {
            logger.error("An error occurred on connection {}:", conn.getRemoteSocketAddress(), ex);
            connections.remove(conn); // エラーが発生した接続は削除するのが安全です
        } else {
            logger.error("An error occurred:", ex);
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
        synchronized (connections) {
            for (WebSocket conn : connections) {
                if (conn != null && conn.isOpen() && !conn.equals(exclude)) {
                    conn.send(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 8887; // サーバーが使用するポート番号
        try {
            GhostModServer server = new GhostModServer(port);
            server.start(); // サーバーを起動
        } catch (Exception e) {
            logger.error("Failed to start WebSocket server", e);
        }
    }
}