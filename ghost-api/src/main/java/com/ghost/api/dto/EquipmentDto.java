package com.ghost.api.dto;

import com.ghost.api.dto.item.ItemDto;

import java.util.Objects;

/**
 * プレイヤーの装備状態を転送するための不変クラス。
 * 各スロットには簡略化された ItemDto が格納されます。
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class EquipmentDto {
    private final ItemDto mainHand;
    private final ItemDto offHand;
    private final ItemDto head;
    private final ItemDto chest;
    private final ItemDto legs;
    private final ItemDto feet;

    public static final Vec2Dto ZERO = new Vec2Dto(0.0f, 0.0f);

    public EquipmentDto(ItemDto mainHand, ItemDto offHand, ItemDto head, ItemDto chest, ItemDto legs, ItemDto feet) {
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
    }

    /**
     * すべての装備スロットが空（minecraft:air）の状態を表現するデフォルトコンストラクタ。
     */
    public EquipmentDto() {
        this(ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR,
                ItemDto.ITEM_AIR);
    }

    public ItemDto mainHand() {
        return mainHand;
    }

    public ItemDto offHand() {
        return offHand;
    }

    public ItemDto head() {
        return head;
    }

    public ItemDto chest() {
        return chest;
    }

    public ItemDto legs() {
        return legs;
    }

    public ItemDto feet() {
        return feet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EquipmentDto that = (EquipmentDto) o;
        return Objects.equals(mainHand, that.mainHand) &&
                Objects.equals(offHand, that.offHand) &&
                Objects.equals(head, that.head) &&
                Objects.equals(chest, that.chest) &&
                Objects.equals(legs, that.legs) &&
                Objects.equals(feet, that.feet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainHand, offHand, head, chest, legs, feet);
    }

    @Override
    public String toString() {
        return "EquipmentDto{" +
                "mainHand=" + mainHand +
                ", offHand=" + offHand +
                ", head=" + head +
                ", chest=" + chest +
                ", legs=" + legs +
                ", feet=" + feet +
                '}';
    }
}