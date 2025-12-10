package com.ghost.api.dto;

import java.util.Objects;

/**
 * A simple, immutable data class for transferring player state.
 * This class is independent of Minecraft's libraries and compatible with older
 * Java versions.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class PlayerData {

    private final Vec3Dto pos;
    private final Vec2Dto rot;
    private final String uuid;
    private final String name;
    private final String pose;
    private final String dimension;
    private final byte skinParts;
    private final String swingArm;
    private final boolean swinging;

    /**
     * The main constructor to create a full PlayerData object.
     *
     * @param pos       Player's position vector.
     * @param rot       Player's rotation vector (yaw, pitch).
     * @param uuid      Player's unique identifier.
     * @param name      Player's display name.
     * @param pose      Player's current pose (e.g., "STANDING").
     * @param dimension Player's current dimension (e.g., "minecraft:overworld").
     * @param skinParts Bitmask of enabled skin parts.
     */
    public PlayerData(
            Vec3Dto pos,
            Vec2Dto rot,
            String uuid,
            String name,
            String pose,
            String dimension,
            byte skinParts,
            String swingArm,
            boolean swinging
    ) {
        this.pos = pos;
        this.rot = rot;
        this.uuid = uuid;
        this.name = name;
        this.pose = pose;
        this.dimension = dimension;
        this.skinParts = skinParts;
        this.swingArm = swingArm;
        this.swinging = swinging;
    }

    /**
     * Default constructor for Gson deserialization.
     * Initializes fields with default non-null values to prevent
     * NullPointerExceptions.
     */
    public PlayerData() {
        this(Vec3Dto.ZERO, Vec2Dto.ZERO, "", "", "STANDING", "", (byte) 127, "MAIN_HAND", false);
    }

    // --- Accessors (Getters) ---

    public Vec3Dto pos() {
        return this.pos;
    }

    public Vec2Dto rot() {
        return this.rot;
    }

    public String uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public String pose() {
        return this.pose;
    }

    public String dimension() {
        return this.dimension;
    }

    public byte skinParts() {
        return this.skinParts;
    }

    public boolean swinging() {
        return this.swinging;
    }

    public String swingArm() {
        return this.swingArm;
    }

    // --- Utility Methods (equals, hashCode, toString) ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PlayerData that = (PlayerData) o;
        return Objects.equals(pos, that.pos) &&
                Objects.equals(rot, that.rot) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(pose, that.pose) &&
                Objects.equals(dimension, that.dimension) &&
                skinParts == that.skinParts &&
                Objects.equals(swingArm, that.swingArm) &&
                swinging == that.swinging;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, rot, uuid, name, pose, dimension, skinParts, swingArm, swinging);
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "pos=" + pos +
                ", rot=" + rot +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", pose='" + pose + '\'' +
                ", dimension='" + dimension + '\'' +
                ", skinParts=" + skinParts + '\'' +
                ", swingArm=" + swingArm + '\'' +
                ", swinging=" + swinging +
                '}';
    }
}