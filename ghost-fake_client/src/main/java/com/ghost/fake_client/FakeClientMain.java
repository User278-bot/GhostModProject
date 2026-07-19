package com.ghost.fake_client;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.net.GhostSyncService;
import com.ghost.api.packet.GhostPacket;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.ghost.api.registry.IGhostRegistry;
import com.ghost.util.SerializationUtil;
import com.ghost.registry.InMemoryGhostRegistry;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static com.ghost.api.packet.MessageType.UPDATE;

public class FakeClientMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeClientMain.class);
    private static final IGhostRegistry GHOST_REGISTRY = new InMemoryGhostRegistry();

    public static void main(String[] args) {
        try {
            // --- コマンドライン引数の定義 ---
            Options options = new Options();
            options.addOption(Option.builder().longOpt("uuid").hasArg().desc("Player UUID").build());
            options.addOption(Option.builder().longOpt("uri").hasArg().desc("Server URI").build());
            options.addOption(Option.builder().longOpt("password").hasArg().desc("Server Password").build());

            // --- 引数の解析 ---
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd;
            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                LOGGER.error("Failed to parse command line arguments: {}", e.getMessage());
                return;
            }

            // --- 接続情報の準備 ---
            String playerUuid = cmd.getOptionValue("uuid", java.util.UUID.randomUUID().toString());
            String serverUriString = cmd.getOptionValue("uri", "ws://localhost:8887");
            String password = cmd.getOptionValue("password", "changeme");

            URI serverUri = new URI(serverUriString);
            String playerName = "TestBot-" + playerUuid.substring(0, 8);
            LOGGER.info("Starting test client: {} (UUID: {})", playerName, playerUuid);

            // --- 依存関係の組み立て ---
            var ghostSyncService = new GhostSyncService(GHOST_REGISTRY);

            LOGGER.info("Connecting to server: {} ...", serverUri);

            // 接続が完了するまで最大5秒間待機する
            var connected = ghostSyncService.connectBlocking(serverUri, password, 5, TimeUnit.SECONDS);

            if (!connected) {
                LOGGER.error("Failed to connect to the server. Exiting.");
                return;
            }

            // --- メインループ：定期的にデータを送信 ---
            long startTime = System.currentTimeMillis();
            while (ghostSyncService.isConnected()) {
                long elapsedTime = System.currentTimeMillis() - startTime;

                PlayerData data = createDummyData(playerName, playerUuid, elapsedTime);
                var packet = new GhostPacket<>(UPDATE, data);
                ghostSyncService.sendPacket(packet);

                var msg = SerializationUtil.serializePacket(packet);
                LOGGER.info("Sent: {}", msg);

                // 3. 200ミリ秒 (5回/秒) ごとに送信
                Thread.sleep(200);
            }

        } catch (Exception ex) {
            LOGGER.error("An error occurred:", ex);
        } finally {
            LOGGER.info("Test client finished.");
        }
    }

    /**
     * 時間の経過と共に円を描くように動く、ダミーのPlayerDataを生成します。
     *
     * @param name       プレイヤー名
     * @param uuid       UUID
     * @param timeMillis 経過時間 (ミリ秒)
     * @return 生成されたPlayerData
     */
    private static PlayerData createDummyData(String name, String uuid, long timeMillis) {
        // 10秒で1周する円運動をシミュレート
        double angle = (timeMillis / 10000.0) * 2 * Math.PI;
        double radius = 5.0; // 半径5ブロック

        double x = -370 + Math.cos(angle) * radius;
        double y = 88.0;
        double z = -90 + Math.sin(angle) * radius;

        // プレイヤーの向きも円の外側を向くように計算
        float yaw = (float) Math.toDegrees(-angle) + 90;

        Vec3Dto pos = new Vec3Dto(x, y, z);
        Vec2Dto rot = new Vec2Dto(yaw, 0f); // pitchは0で固定

        // PlayerData DTOを直接インスタンス化
        return new PlayerData(pos, rot, uuid, name, "STANDING", "minecraft:overworld", (byte) 127, "MAIN_HAND", 0,new EquipmentDto());
    }
}