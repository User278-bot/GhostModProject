package com.ghost.api.dto;

/**
 * データ転送用の軽量な不変 3D ベクトルレコード。
 * Minecraft ライブラリ非依存であり、Vec3 互換の数学演算を提供します。
 */
@SuppressWarnings("unused")
public record Vec3Dto(double x, double y, double z) {
    /** ゼロベクトル (0, 0, 0) */
    public static final Vec3Dto ZERO = new Vec3Dto(0.0, 0.0, 0.0);

    public Vec3Dto() {
        this(0.0, 0.0, 0.0);
    }

    // --- 数学演算 ---

    public Vec3Dto add(Vec3Dto vec) {
        return new Vec3Dto(this.x + vec.x, this.y + vec.y, this.z + vec.z);
    }

    public Vec3Dto subtract(Vec3Dto vec) {
        return new Vec3Dto(this.x - vec.x, this.y - vec.y, this.z - vec.z);
    }

    public Vec3Dto multiply(double scalar) {
        return new Vec3Dto(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public double distanceToSqr(Vec3Dto vec) {
        double dx = this.x - vec.x;
        double dy = this.y - vec.y;
        double dz = this.z - vec.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(Vec3Dto vec) {
        return Math.sqrt(this.distanceToSqr(vec));
    }

    public Vec3Dto normalize() {
        double length = this.length();
        if (length < 1.0E-4) {
            return ZERO;
        }
        return this.multiply(1.0 / length);
    }

    public double dot(Vec3Dto vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public Vec3Dto cross(Vec3Dto vec) {
        return new Vec3Dto(
                this.y * vec.z - this.z * vec.y,
                this.z * vec.x - this.x * vec.z,
                this.x * vec.y - this.y * vec.x
        );
    }

    public double length() {
        return Math.sqrt(this.lengthSqr());
    }

    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
}