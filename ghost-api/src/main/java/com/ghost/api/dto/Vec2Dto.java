package com.ghost.api.dto;

import java.util.Objects;

/**
 * A simple, immutable 2D vector class for data transfer.
 * This class is independent of Minecraft's libraries.
 * Methods are designed to be compatible with Minecraft's Vec2 class.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class Vec2Dto {
    /**
     * A constant for the zero vector (0, 0).
     */
    public static final Vec2Dto ZERO = new Vec2Dto(0.0f, 0.0f);
    private final float x, y;

    public Vec2Dto(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2Dto() {
        this.x = 0f;
        this.y = 0f;
    }

    public float x(){
        return x;
    }

    public float y(){
        return y;
    }

    // --- Mathematical Operations ---

    /**
     * Returns a new vector that is the result of adding the given vector to this vector.
     */
    public Vec2Dto add(Vec2Dto vec) {
        return new Vec2Dto(this.x + vec.x, this.y + vec.y);
    }

    public Vec2Dto subtract(Vec2Dto vec) {
        return new Vec2Dto(this.x - vec.x, this.y - vec.y);
    }

    /**
     * Returns a new vector that is the result of multiplying this vector by the given scalar.
     */
    public Vec2Dto multiply(float scalar) {
        return new Vec2Dto(this.x * scalar, this.y * scalar);
    }


    /**
     * Calculates the squared distance between this vector and another vector.
     */
    public float distanceToSqr(Vec2Dto vec) {
        float dx = this.x - vec.x;
        float dy = this.y - vec.y;
        return dx * dx + dy * dy;
    }

    public double distanceTo(Vec2Dto vec) {
        return Math.sqrt(this.distanceToSqr(vec));
    }

    public Vec2Dto normalize() {
        float length = this.length();
        if (Float.compare(length, 0f) == 0) {
            return ZERO;
        }
        return this.multiply(1.0f / length);
    }

    public double dot(Vec2Dto vec) {
        return this.x * vec.x + this.y * vec.y;
    }

    public float cross(Vec2Dto vec) {
        return this.x * vec.y - this.y * vec.x;
    }

    /**
     * Calculates the length (magnitude) of this vector.
     */
    public float length() {
        return (float) Math.sqrt(this.lengthSqr());
    }

    /**
     * Calculates the squared length of this vector.
     */
    public float lengthSqr() {
        return this.x * this.x + this.y * this.y;
    }

    // --- Utility Methods (equals, hashCode, toString) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec2Dto vec2Dto = (Vec2Dto) o;
        return Float.compare(vec2Dto.x, x) == 0 &&
                Float.compare(vec2Dto.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y); // java.util.Objects をインポート
    }

    @Override
    public String toString() {
        return "Vec2Dto{" + "x=" + x + ", y=" + y + '}';
    }
}