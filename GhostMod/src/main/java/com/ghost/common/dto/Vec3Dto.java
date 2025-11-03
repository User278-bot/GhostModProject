package com.ghost.common.dto;

import java.util.Objects;

/**
 * A simple, immutable 3D vector class for data transfer.
 * This class is independent of Minecraft's libraries and compatible with older Java versions.
 * Methods are designed to be compatible with Minecraft's Vec3 class.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class Vec3Dto {

    /**
     * A constant for the zero vector (0, 0, 0).
     */
    public static final Vec3Dto ZERO = new Vec3Dto(0.0, 0.0, 0.0);

    private final double x;
    private final double y;
    private final double z;

    /**
     * Creates a new vector with the given coordinates.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    public Vec3Dto(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Default constructor for Gson deserialization. Creates a zero vector.
     */
    public Vec3Dto() {
        this(0.0, 0.0, 0.0);
    }

    // --- Accessors (Getters) ---

    /**
     * Returns the x-coordinate of this vector.
     * @return the x-coordinate.
     */
    public double x() {
        return this.x;
    }

    /**
     * Returns the y-coordinate of this vector.
     * @return the y-coordinate.
     */
    public double y() {
        return this.y;
    }

    /**
     * Returns the z-coordinate of this vector.
     * @return the z-coordinate.
     */
    public double z() {
        return this.z;
    }

    // --- Mathematical Operations ---

    /**
     * Returns a new vector that is the result of adding the given vector to this vector.
     */
    public Vec3Dto add(Vec3Dto vec) {
        return new Vec3Dto(this.x + vec.x, this.y + vec.y, this.z + vec.z);
    }

    /**
     * Returns a new vector that is the result of subtracting the given vector from this vector.
     */
    public Vec3Dto subtract(Vec3Dto vec) {
        return new Vec3Dto(this.x - vec.x, this.y - vec.y, this.z - vec.z);
    }

    /**
     * Returns a new vector that is the result of multiplying this vector by the given scalar.
     */
    public Vec3Dto multiply(double scalar) {
        return new Vec3Dto(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    /**
     * Calculates the squared distance between this vector and another vector.
     * This is faster than distanceTo as it avoids a square root operation.
     */
    public double distanceToSqr(Vec3Dto vec) {
        double dx = this.x - vec.x;
        double dy = this.y - vec.y;
        double dz = this.z - vec.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance between this vector and another vector.
     */
    public double distanceTo(Vec3Dto vec) {
        return Math.sqrt(this.distanceToSqr(vec));
    }

    /**
     * Returns a new vector with the same direction as this vector but with a length of 1.
     * If the length is zero, it returns a zero vector.
     */
    public Vec3Dto normalize() {
        double length = this.length();
        if (length < 1.0E-4) {
            return ZERO;
        }
        return this.multiply(1.0 / length);
    }

    /**
     * Calculates the dot product of this vector and another vector.
     */
    public double dot(Vec3Dto vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    /**
     * Returns a new vector that is the result of the cross product of this vector and another vector.
     */
    public Vec3Dto cross(Vec3Dto vec) {
        return new Vec3Dto(
                this.y * vec.z - this.z * vec.y,
                this.z * vec.x - this.x * vec.z,
                this.x * vec.y - this.y * vec.x
        );
    }

    /**
     * Calculates the length (magnitude) of this vector.
     */
    public double length() {
        return Math.sqrt(this.lengthSqr());
    }

    /**
     * Calculates the squared length of this vector.
     * This is faster than length() as it avoids a square root operation.
     */
    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    // --- Utility Methods (equals, hashCode, toString) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3Dto vec3Dto = (Vec3Dto) o;
        return Double.compare(vec3Dto.x, x) == 0 &&
                Double.compare(vec3Dto.y, y) == 0 &&
                Double.compare(vec3Dto.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vec3Dto{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}