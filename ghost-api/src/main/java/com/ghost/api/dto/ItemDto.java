package com.ghost.api.dto;

import java.util.Objects;

/**
 * アイテムの基本情報（アイテムID）のみを転送するための不変クラス。
 * 段階的実装の第一段階として、バージョン間の差異がないIDのみを同期します。
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class ItemDto {

    private final String id;
    public static final ItemDto ITEM_AIR = new ItemDto();

    /**
     * @param id MinecraftのアイテムID (例: "minecraft:stone", "minecraft:air")
     */
    public ItemDto(String id) {
        this.id = id;
    }

    /**
     * Gsonデシリアライズ時、および空のアイテムスロットを表現するためのデフォルトコンストラクタ。
     * バニラの仕様に則り、"minecraft:air" をデフォルトとします。
     */
    public ItemDto() {
        this("minecraft:air");
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ItemDto itemDto = (ItemDto) o;
        return Objects.equals(id, itemDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ItemDto{" +
                "id='" + id + '\'' +
                '}';
    }
}