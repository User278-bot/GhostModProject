package com.ghost.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerData")
class PlayerDataTest {

    @Nested
    @DisplayName("コンストラクタ")
    class ConstructorTests {

        @Test
        @DisplayName("全引数コンストラクタで正しくインスタンスが作成される")
        void shouldCreateInstanceWithAllArgs() {
            // 準備・実行
            PlayerData data = new PlayerData(
                    new Vec3Dto(100.0, 64.0, -200.0),
                    new Vec2Dto(45.0f, 90.0f),
                    "test-uuid",
                    "TestPlayer",
                    "STANDING",
                    "minecraft:overworld",
                    (byte) 127,
                    "MAIN_HAND",
                    0
            );

            // 検証
            assertEquals(100.0, data.pos().x(), 0.001);
            assertEquals(64.0, data.pos().y(), 0.001);
            assertEquals(-200.0, data.pos().z(), 0.001);
            assertEquals(45.0, data.rot().x(), 0.001);
            assertEquals(90.0, data.rot().y(), 0.001);
            assertEquals("test-uuid", data.uuid());
            assertEquals("TestPlayer", data.name());
            assertEquals("STANDING", data.pose());
            assertEquals("minecraft:overworld", data.dimension());
            assertEquals(127, data.skinParts());
        }

        @Test
        @DisplayName("デフォルトコンストラクタでデフォルト値が設定される")
        void shouldCreateInstanceWithDefaults() {
            // 実行
            PlayerData data = new PlayerData();

            // 検証
            assertNotNull(data.pos());
            assertNotNull(data.rot());
            assertEquals(Vec3Dto.ZERO, data.pos());
            assertEquals(Vec2Dto.ZERO, data.rot());
            assertEquals("", data.uuid());
            assertEquals("", data.name());
            assertEquals("STANDING", data.pose());
            assertEquals("", data.dimension());
            assertEquals(127, data.skinParts());
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("同じ値を持つインスタンスは等しい")
        void shouldBeEqualForSameValues() {
            // 準備
            PlayerData data1 = new PlayerData(
                    new Vec3Dto(1.0, 2.0, 3.0),
                    new Vec2Dto(10.0f, 20.0f),
                    "uuid",
                    "Name",
                    "POSE",
                    "dim",
                    (byte) 50,
                    "MAIN_HAND",
                    0);
            PlayerData data2 = new PlayerData(
                    new Vec3Dto(1.0, 2.0, 3.0),
                    new Vec2Dto(10.0f, 20.0f),
                    "uuid",
                    "Name",
                    "POSE",
                    "dim",
                    (byte) 50,
                    "MAIN_HAND",
                    0);

            // 検証
            assertEquals(data1, data2);
            assertEquals(data1.hashCode(), data2.hashCode());
        }

        @Test
        @DisplayName("異なる値を持つインスタンスは等しくない")
        void shouldNotBeEqualForDifferentValues() {
            // 準備
            PlayerData data1 = new PlayerData(
                    new Vec3Dto(1.0, 2.0, 3.0),
                    new Vec2Dto(10.0f, 20.0f),
                    "uuid1",
                    "Name1",
                    "STANDING",
                    "minecraft:overworld",
                    (byte) 127,
                    "MAIN_HAND",
                    0);
            PlayerData data2 = new PlayerData(
                    new Vec3Dto(1.0, 2.0, 3.0),
                    new Vec2Dto(10.0f, 20.0f),
                    "uuid2", // 異なるUUID
                    "Name1",
                    "STANDING",
                    "minecraft:overworld",
                    (byte) 127,
                    "MAIN_HAND",
                    0);

            // 検証
            assertNotEquals(data1, data2);
        }

        @Test
        @DisplayName("自身との比較は等しい")
        void shouldBeEqualToItself() {
            // 準備
            PlayerData data = new PlayerData();

            // 検証
            assertEquals(data, data);
        }

        @Test
        @DisplayName("nullとの比較は等しくない")
        void shouldNotBeEqualToNull() {
            // 準備
            PlayerData data = new PlayerData();

            // 検証
            assertNotEquals(null, data);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toStringが全フィールドを含む")
        void shouldContainAllFields() {
            // 準備
            PlayerData data = new PlayerData(
                    new Vec3Dto(100.0, 64.0, 200.0),
                    new Vec2Dto(45.0f, 90.0f),
                    "test-uuid",
                    "TestPlayer",
                    "CROUCHING",
                    "minecraft:the_nether",
                    (byte) 63,
                    "MAIN_HAND",
                    0);

            // 実行
            String str = data.toString();

            // 検証
            assertTrue(str.contains("test-uuid"));
            assertTrue(str.contains("TestPlayer"));
            assertTrue(str.contains("CROUCHING"));
            assertTrue(str.contains("minecraft:the_nether"));
        }
    }

    @Nested
    @DisplayName("skinParts ビットマスク")
    class SkinPartsTests {

        @Test
        @DisplayName("skinParts=127は全パーツ表示")
        void shouldHaveAllPartsVisible() {
            // 127 = 0b01111111 (全パーツ有効)
            PlayerData data = new PlayerData(
                    Vec3Dto.ZERO, Vec2Dto.ZERO,
                    "uuid", "name", "STANDING", "dim", (byte) 127,
                    "MAIN_HAND",
                    0);

            assertEquals(127, data.skinParts());
        }

        @Test
        @DisplayName("skinParts=0は全パーツ非表示")
        void shouldHaveNoPartsVisible() {
            PlayerData data = new PlayerData(
                    Vec3Dto.ZERO, Vec2Dto.ZERO,
                    "uuid", "name", "STANDING", "dim", (byte) 0,
                    "MAIN_HAND",
                    0);

            assertEquals(0, data.skinParts());
        }
    }
}
