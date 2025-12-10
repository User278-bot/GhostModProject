package com.ghost.registry;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

/**
 * InMemoryGhostRegistry のユニットテストクラスです。
 * JUnit 5 を使用して、クラスの動作が仕様通りかを確認します。
 */
class InMemoryGhostRegistryTest {

    private InMemoryGhostRegistry registry;

    /**
     * @BeforeEach: 各テストメソッド（@Test）が実行される「前」に毎回呼ばれます。
     * ここでテスト対象のインスタンスを初期化することで、
     * 常にクリーンな状態でテストを開始できます。
     */
    @BeforeEach
    void setUp() {
        registry = new InMemoryGhostRegistry();
    }

    /**
     * @Test: このメソッドがテストケースであることを示します。
     * メソッド名は「何をテストしているか」が分かるように命名するのが一般的です。
     */
    @Test
    void initialState_ShouldBeEmpty() {
        // 実行 (Act)
        Collection<PlayerData> ghosts = registry.getAllGhosts();

        // 検証 (Assert)
        // Assertions.assertTrue: 条件が true であることを確認します。
        Assertions.assertTrue(ghosts.isEmpty(), "初期状態ではゴーストは0人であるべき");
    }

    @Test
    void updateGhost_AddsNewGhost() {
        // 準備 (Arrange)
        PlayerData data = createTestData("uuid-1", "Player1");

        // 実行 (Act)
        registry.updateGhost(data);

        // 検証 (Assert)
        Collection<PlayerData> ghosts = registry.getAllGhosts();
        // Assertions.assertEquals(期待値, 実際の値): 値が等しいか確認します。
        Assertions.assertEquals(1, ghosts.size(), "ゴーストが1人追加されているべき");

        PlayerData storedData = ghosts.iterator().next();
        Assertions.assertEquals("uuid-1", storedData.uuid());
        Assertions.assertEquals("Player1", storedData.name());
    }

    @Test
    void updateGhost_UpdatesExistingGhost() {
        // 準備 (Arrange)
        PlayerData initialData = createTestData("uuid-1", "Player1");
        registry.updateGhost(initialData);

        // 同じUUIDで新しいデータを準備（位置を変更）
        PlayerData updatedData = new PlayerData(
                new Vec3Dto(10, 20, 30),
                new Vec2Dto(0, 0),
                "uuid-1",
                "Player1",
                "STANDING",
                "minecraft:overworld",
                (byte) 127);

        // 実行 (Act)
        registry.updateGhost(updatedData);

        // 検証 (Assert)
        Collection<PlayerData> ghosts = registry.getAllGhosts();
        Assertions.assertEquals(1, ghosts.size(), "更新なので人数は増えないべき");

        PlayerData storedData = ghosts.iterator().next();
        Assertions.assertEquals(10, storedData.pos().x(), "X座標が更新されているべき");
    }

    @Test
    void removeGhost_RemovesExistingGhost() {
        // 準備 (Arrange)
        PlayerData data = createTestData("uuid-1", "Player1");
        registry.updateGhost(data);

        // 実行 (Act)
        Optional<PlayerData> removed = registry.removeGhost("uuid-1");

        // 検証 (Assert)
        Assertions.assertTrue(removed.isPresent(), "削除されたデータが返されるべき");
        Assertions.assertEquals("Player1", removed.get().name());
        Assertions.assertTrue(registry.getAllGhosts().isEmpty(), "削除後はリストが空になるべき");
    }

    @Test
    void clear_RemovesAllGhosts() {
        // 準備 (Arrange)
        registry.updateGhost(createTestData("uuid-1", "Player1"));
        registry.updateGhost(createTestData("uuid-2", "Player2"));

        // 実行 (Act)
        registry.clear();

        // 検証 (Assert)
        Assertions.assertTrue(registry.getAllGhosts().isEmpty(), "clear後は全員消えるべき");
    }

    // --- ヘルパーメソッド ---
    // テストデータの作成を簡単にするためのメソッド
    private PlayerData createTestData(String uuid, String name) {
        return new PlayerData(
                new Vec3Dto(0, 0, 0),
                new Vec2Dto(0, 0),
                uuid,
                name,
                "STANDING",
                "minecraft:overworld",
                (byte) 127);
    }
}
