package com.ghost.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vec3Dto")
class Vec3DtoTest {

    @Nested
    @DisplayName("コンストラクタと定数")
    class ConstructorAndConstantsTests {

        @Test
        @DisplayName("3引数コンストラクタで正しくインスタンスが作成される")
        void shouldCreateInstanceWithThreeArgs() {
            Vec3Dto vec = new Vec3Dto(1.5, 2.5, 3.5);

            assertEquals(1.5, vec.x(), 0.0001);
            assertEquals(2.5, vec.y(), 0.0001);
            assertEquals(3.5, vec.z(), 0.0001);
        }

        @Test
        @DisplayName("ZEROは原点ベクトル")
        void zeroShouldBeOrigin() {
            assertEquals(0.0, Vec3Dto.ZERO.x(), 0.0001);
            assertEquals(0.0, Vec3Dto.ZERO.y(), 0.0001);
            assertEquals(0.0, Vec3Dto.ZERO.z(), 0.0001);
        }

        @Test
        @DisplayName("デフォルトコンストラクタはZEROと同じ")
        void defaultConstructorShouldBeZero() {
            Vec3Dto vec = new Vec3Dto();

            assertEquals(0.0, vec.x(), 0.0001);
            assertEquals(0.0, vec.y(), 0.0001);
            assertEquals(0.0, vec.z(), 0.0001);
        }
    }

    @Nested
    @DisplayName("数学演算")
    class MathOperationsTests {

        @Test
        @DisplayName("addで正しく加算される")
        void addShouldWork() {
            Vec3Dto v1 = new Vec3Dto(1.0, 2.0, 3.0);
            Vec3Dto v2 = new Vec3Dto(4.0, 5.0, 6.0);

            Vec3Dto result = v1.add(v2);

            assertEquals(5.0, result.x(), 0.0001);
            assertEquals(7.0, result.y(), 0.0001);
            assertEquals(9.0, result.z(), 0.0001);
        }

        @Test
        @DisplayName("subtractで正しく減算される")
        void subtractShouldWork() {
            Vec3Dto v1 = new Vec3Dto(5.0, 7.0, 9.0);
            Vec3Dto v2 = new Vec3Dto(1.0, 2.0, 3.0);

            Vec3Dto result = v1.subtract(v2);

            assertEquals(4.0, result.x(), 0.0001);
            assertEquals(5.0, result.y(), 0.0001);
            assertEquals(6.0, result.z(), 0.0001);
        }

        @Test
        @DisplayName("multiplyで正しくスカラー乗算される")
        void multiplyShouldWork() {
            Vec3Dto v = new Vec3Dto(2.0, 3.0, 4.0);

            Vec3Dto result = v.multiply(3.0);

            assertEquals(6.0, result.x(), 0.0001);
            assertEquals(9.0, result.y(), 0.0001);
            assertEquals(12.0, result.z(), 0.0001);
        }

        @Test
        @DisplayName("lengthで正しくベクトルの長さが計算される")
        void lengthShouldWork() {
            Vec3Dto v = new Vec3Dto(3.0, 4.0, 0.0);

            assertEquals(5.0, v.length(), 0.0001);
        }

        @Test
        @DisplayName("distanceToで正しく距離が計算される")
        void distanceToShouldWork() {
            Vec3Dto v1 = new Vec3Dto(0.0, 0.0, 0.0);
            Vec3Dto v2 = new Vec3Dto(3.0, 4.0, 0.0);

            assertEquals(5.0, v1.distanceTo(v2), 0.0001);
        }

        @Test
        @DisplayName("normalizeで単位ベクトルになる")
        void normalizeShouldWork() {
            Vec3Dto v = new Vec3Dto(3.0, 4.0, 0.0);

            Vec3Dto result = v.normalize();

            assertEquals(1.0, result.length(), 0.0001);
            assertEquals(0.6, result.x(), 0.0001);
            assertEquals(0.8, result.y(), 0.0001);
        }

        @Test
        @DisplayName("ゼロベクトルのnormalizeはゼロを返す")
        void normalizeZeroShouldReturnZero() {
            Vec3Dto result = Vec3Dto.ZERO.normalize();

            assertEquals(Vec3Dto.ZERO, result);
        }

        @Test
        @DisplayName("dotで正しく内積が計算される")
        void dotShouldWork() {
            Vec3Dto v1 = new Vec3Dto(1.0, 2.0, 3.0);
            Vec3Dto v2 = new Vec3Dto(4.0, 5.0, 6.0);

            // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
            assertEquals(32.0, v1.dot(v2), 0.0001);
        }

        @Test
        @DisplayName("crossで正しく外積が計算される")
        void crossShouldWork() {
            Vec3Dto v1 = new Vec3Dto(1.0, 0.0, 0.0);
            Vec3Dto v2 = new Vec3Dto(0.0, 1.0, 0.0);

            Vec3Dto result = v1.cross(v2);

            assertEquals(0.0, result.x(), 0.0001);
            assertEquals(0.0, result.y(), 0.0001);
            assertEquals(1.0, result.z(), 0.0001); // i x j = k
        }
    }

    @Nested
    @DisplayName("equals / hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("同じ座標のベクトルは等しい")
        void shouldBeEqualForSameCoordinates() {
            Vec3Dto vec1 = new Vec3Dto(10.0, 20.0, 30.0);
            Vec3Dto vec2 = new Vec3Dto(10.0, 20.0, 30.0);

            assertEquals(vec1, vec2);
            assertEquals(vec1.hashCode(), vec2.hashCode());
        }

        @Test
        @DisplayName("異なる座標のベクトルは等しくない")
        void shouldNotBeEqualForDifferentCoordinates() {
            Vec3Dto vec1 = new Vec3Dto(10.0, 20.0, 30.0);
            Vec3Dto vec2 = new Vec3Dto(10.0, 20.0, 31.0);

            assertNotEquals(vec1, vec2);
        }

        @Test
        @DisplayName("負の座標も正しく扱える")
        void shouldHandleNegativeCoordinates() {
            Vec3Dto vec = new Vec3Dto(-100.5, -64.0, -200.5);

            assertEquals(-100.5, vec.x(), 0.0001);
            assertEquals(-64.0, vec.y(), 0.0001);
            assertEquals(-200.5, vec.z(), 0.0001);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toStringが座標を含む")
        void shouldContainCoordinates() {
            Vec3Dto vec = new Vec3Dto(123.45, 678.90, -111.22);
            String str = vec.toString();

            assertTrue(str.contains("123.45") || str.contains("123"));
            assertTrue(str.contains("678.9") || str.contains("678"));
        }
    }
}
