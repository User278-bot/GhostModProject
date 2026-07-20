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
    private final String mainArm;
    private final int swingTime;
    private final EquipmentDto equipment; // 追加: 装備データの同期
    private final boolean isUsingItem;
    private final String activeHand;

    /**
     * The main constructor to create a full PlayerData object.
     *
     * @param pos       Player's position vector.
     * @param rot       Player's rotation vector (yaw, pitch).
     * @param uuid      Player's unique identifier.
     * @param name      Player's display name.
     * @param pose      Player's current pose (e.g., "STANDING").
     * @param dimension Player's current dimension (e.g., "minecraft:overworld").
     * @param skinParts   Bitmask of enabled skin parts.
     * @param mainArm     Player's dominant arm ("RIGHT" or "LEFT").
     * @param swingTime   Timer for the arm swing animation.
     * @param equipment   Player's equipment set (helmet, chestplate, etc.).
     * @param isUsingItem Whether the player is currently using an item.
     * @param activeHand  The hand currently interacting ("MAIN_HAND" or "OFF_HAND").
     */
    public PlayerData(
            Vec3Dto pos,
            Vec2Dto rot,
            String uuid,
            String name,
            String pose,
            String dimension,
            byte skinParts,
            String mainArm,
            int swingTime,
            EquipmentDto equipment,
            boolean isUsingItem,
            String activeHand) {
        this.pos = pos;
        this.rot = rot;
        this.uuid = uuid;
        this.name = name;
        this.pose = pose;
        this.dimension = dimension;
        this.skinParts = skinParts;
        this.mainArm = mainArm;
        this.swingTime = swingTime;
        this.equipment = equipment;
        this.isUsingItem = isUsingItem;
        this.activeHand = activeHand;
    }

    /**
     * Default constructor for Gson deserialization.
     * Initializes fields with default non-null values to prevent
     * NullPointerExceptions.
     */
    public PlayerData() {
        this(Vec3Dto.ZERO, Vec2Dto.ZERO, "", "", "STANDING", "", (byte) 127, "RIGHT", 0, new EquipmentDto(), false, "MAIN_HAND");
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

    public int swingTime() {
        return this.swingTime;
    }

    public String mainArm() {
        return this.mainArm;
    }

    public boolean isUsingItem() {
        return this.isUsingItem;
    }

    public String activeHand() {
        return this.activeHand;
    }

    public EquipmentDto equipment() {
        return this.equipment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PlayerData that = (PlayerData) o;
        return skinParts == that.skinParts &&
                swingTime == that.swingTime &&
                isUsingItem == that.isUsingItem &&
                Objects.equals(pos, that.pos) &&
                Objects.equals(rot, that.rot) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(pose, that.pose) &&
                Objects.equals(dimension, that.dimension) &&
                Objects.equals(mainArm, that.mainArm) &&
                Objects.equals(equipment, that.equipment) &&
                Objects.equals(activeHand, that.activeHand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, rot, uuid, name, pose, dimension, skinParts, mainArm, swingTime, equipment, isUsingItem, activeHand);
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
                ", skinParts=" + skinParts +
                ", mainArm='" + mainArm + '\'' +
                ", swingTime=" + swingTime +
                ", equipment=" + equipment +
                ", isUsingItem=" + isUsingItem +
                ", activeHand='" + activeHand + '\'' +
                '}';
    }
}