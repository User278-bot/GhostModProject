package com.ghost.api.dto;

/**
 * データ転送用の軽量な不変 2D ベクトルレコード。
 * Minecraft ライブラリ非依存であり、回転角として使用する場合は x=Pitch、y=Yaw を格納します。
 */
@SuppressWarnings("unused")
public record Vec2Dto(float x, float y) {
    /** ゼロベクトル (0, 0) */
    public static final Vec2Dto ZERO = new Vec2Dto(0.0f, 0.0f);

    public Vec2Dto() {
        this(0.0f, 0.0f);
    }

    // --- 数学演算 ---

    public Vec2Dto add(Vec2Dto vec) {
        return new Vec2Dto(this.x + vec.x, this.y + vec.y);
    }

    public Vec2Dto subtract(Vec2Dto vec) {
        return new Vec2Dto(this.x - vec.x, this.y - vec.y);
    }

    public Vec2Dto multiply(float scalar) {
        return new Vec2Dto(this.x * scalar, this.y * scalar);
    }

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

    public float length() {
        return (float) Math.sqrt(this.lengthSqr());
    }

    public float lengthSqr() {
        return this.x * this.x + this.y * this.y;
    }
}