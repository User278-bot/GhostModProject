package com.ghost.api.dto;

import com.ghost.api.dto.item.ItemDto;

/**
 * プレイヤーの装備状態を転送するための不変レコード。
 * 各スロットには簡略化された ItemDto が格納されます。
 */
public record EquipmentDto(
        ItemDto mainHand,
        ItemDto offHand,
        ItemDto head,
        ItemDto chest,
        ItemDto legs,
        ItemDto feet
) {
    /** 全スロットが空（minecraft:air）の装備状態を表す定数 */
    public static final EquipmentDto EMPTY = new EquipmentDto();

    public EquipmentDto {
        mainHand = mainHand != null ? mainHand : ItemDto.ITEM_AIR;
        offHand = offHand != null ? offHand : ItemDto.ITEM_AIR;
        head = head != null ? head : ItemDto.ITEM_AIR;
        chest = chest != null ? chest : ItemDto.ITEM_AIR;
        legs = legs != null ? legs : ItemDto.ITEM_AIR;
        feet = feet != null ? feet : ItemDto.ITEM_AIR;
    }

    /**
     * すべての装備スロットが空（minecraft:air）の状態を表現するデフォルトコンストラクタ。
     */
    public EquipmentDto() {
        this(ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR, ItemDto.ITEM_AIR,
                ItemDto.ITEM_AIR);
    }
}