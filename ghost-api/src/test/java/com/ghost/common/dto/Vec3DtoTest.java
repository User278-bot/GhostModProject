package com.ghost.common.dto;

import com.ghost.api.dto.Vec3Dto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Vec3DtoTest {

    private static final double DELTA = 0.000001;

    @Test
    void add_ShouldReturnSumOfVectors() {
        Vec3Dto v1 = new Vec3Dto(1.0, 2.0, 3.0);
        Vec3Dto v2 = new Vec3Dto(4.0, 5.0, 6.0);
        Vec3Dto result = v1.add(v2);

        assertEquals(5.0, result.x(), DELTA);
        assertEquals(7.0, result.y(), DELTA);
        assertEquals(9.0, result.z(), DELTA);
    }

    @Test
    void subtract_ShouldReturnDifferenceOfVectors() {
        Vec3Dto v1 = new Vec3Dto(5.0, 7.0, 9.0);
        Vec3Dto v2 = new Vec3Dto(1.0, 2.0, 3.0);
        Vec3Dto result = v1.subtract(v2);

        assertEquals(4.0, result.x(), DELTA);
        assertEquals(5.0, result.y(), DELTA);
        assertEquals(6.0, result.z(), DELTA);
    }

    @Test
    void multiply_ShouldReturnScaledVector() {
        Vec3Dto v = new Vec3Dto(1.0, -2.0, 3.0);
        Vec3Dto result = v.multiply(2.0);

        assertEquals(2.0, result.x(), DELTA);
        assertEquals(-4.0, result.y(), DELTA);
        assertEquals(6.0, result.z(), DELTA);
    }

    @Test
    void distanceTo_ShouldReturnCorrectDistance() {
        Vec3Dto v1 = new Vec3Dto(0.0, 0.0, 0.0);
        Vec3Dto v2 = new Vec3Dto(3.0, 4.0, 0.0);

        assertEquals(5.0, v1.distanceTo(v2), DELTA);
    }

    @Test
    void normalize_ShouldReturnUnitVector() {
        Vec3Dto v = new Vec3Dto(3.0, 0.0, 0.0);
        Vec3Dto result = v.normalize();

        assertEquals(1.0, result.x(), DELTA);
        assertEquals(0.0, result.y(), DELTA);
        assertEquals(0.0, result.z(), DELTA);
        assertEquals(1.0, result.length(), DELTA);
    }

    @Test
    void normalize_ZeroVector_ShouldReturnZero() {
        Vec3Dto v = new Vec3Dto(0.0, 0.0, 0.0);
        Vec3Dto result = v.normalize();

        assertEquals(0.0, result.x(), DELTA);
        assertEquals(0.0, result.y(), DELTA);
        assertEquals(0.0, result.z(), DELTA);
    }

    @Test
    void dot_ShouldReturnDotProduct() {
        Vec3Dto v1 = new Vec3Dto(1.0, 2.0, 3.0);
        Vec3Dto v2 = new Vec3Dto(4.0, -5.0, 6.0);
        // 1*4 + 2*(-5) + 3*6 = 4 - 10 + 18 = 12
        assertEquals(12.0, v1.dot(v2), DELTA);
    }

    @Test
    void cross_ShouldReturnCrossProduct() {
        Vec3Dto v1 = new Vec3Dto(1.0, 0.0, 0.0);
        Vec3Dto v2 = new Vec3Dto(0.0, 1.0, 0.0);
        Vec3Dto result = v1.cross(v2);

        // X cross Y should be Z
        assertEquals(0.0, result.x(), DELTA);
        assertEquals(0.0, result.y(), DELTA);
        assertEquals(1.0, result.z(), DELTA);
    }
}
