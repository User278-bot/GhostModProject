package com.ghost.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vec2Dto")
class Vec2DtoTest {

    @Nested
    @DisplayName("コンストラクタと定数")
    class ConstructorAndConstantsTests {

        @Test
        @DisplayName("2引数コンストラクタで正しくインスタンスが作成される")
        void shouldCreateInstanceWithTwoArgs() {
            Vec2Dto vec = new Vec2Dto(45.0f, 90.0f);

            assertEquals(45.0f, vec.x(), 0.0001f);
            assertEquals(90.0f, vec.y(), 0.0001f);
        }

        @Test
        @DisplayName("ZEROは原点")
        void zeroShouldBeOrigin() {
            assertEquals(0.0f, Vec2Dto.ZERO.x(), 0.0001f);
            assertEquals(0.0f, Vec2Dto.ZERO.y(), 0.0001f);
        }

        @Test
        @DisplayName("デフォルトコンストラクタはZEROと同じ")
        void defaultConstructorShouldBeZero() {
            Vec2Dto vec = new Vec2Dto();

            assertEquals(0.0f, vec.x(), 0.0001f);
            assertEquals(0.0f, vec.y(), 0.0001f);
        }
    }

    @Nested
    @DisplayName("回転値のテスト")
    class RotationTests {

        @Test
        @DisplayName("Pitch/Yaw値を正しく保持する")
        void shouldStorePitchAndYaw() {
            // x = pitch (上下), y = yaw (左右)
            Vec2Dto rot = new Vec2Dto(-90.0f, 180.0f);

            assertEquals(-90.0f, rot.x(), 0.0001f); // 真上を向く
            assertEquals(180.0f, rot.y(), 0.0001f); // 南向き
        }

        @Test
        @DisplayName("360度を超える値も保持できる")
        void shouldHandle360PlusDegrees() {
            Vec2Dto rot = new Vec2Dto(0.0f, 720.0f);

            assertEquals(720.0f, rot.y(), 0.0001f);
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("同じ値のベクトルは等しい")
        void shouldBeEqualForSameValues() {
            Vec2Dto vec1 = new Vec2Dto(30.0f, 60.0f);
            Vec2Dto vec2 = new Vec2Dto(30.0f, 60.0f);

            assertEquals(vec1, vec2);
            assertEquals(vec1.hashCode(), vec2.hashCode());
        }

        @Test
        @DisplayName("異なる値のベクトルは等しくない")
        void shouldNotBeEqualForDifferentValues() {
            Vec2Dto vec1 = new Vec2Dto(30.0f, 60.0f);
            Vec2Dto vec2 = new Vec2Dto(30.0f, 61.0f);

            assertNotEquals(vec1, vec2);
        }
    }
}
