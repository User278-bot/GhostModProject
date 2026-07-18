package com.ghost.api.dto.item;

import com.ghost.api.dto.item.components.CustomModelDataDto;
import com.ghost.api.dto.item.components.TrimDto;

import java.util.Objects;

/**
 * アイテムのビジュアル情報を転送するための軽量な不変クラス。
 * Builderパターンを採用し、対象のMinecraftバージョンに存在するデータのみを安全に構築可能。
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class ItemDto {

    private final String id;
    private final int damage;
    private final boolean hasGlint;
    private final int color;
    private final String itemModel;
    private final CustomModelDataDto customModelData;
    private final TrimDto trim;

    public static final ItemDto ITEM_AIR = new Builder("minecraft:air").build();

    // デフォルトコンストラクタ（Gson等のデシリアライズ用）
    public ItemDto() {
        this(new Builder("minecraft:air"));
    }

    private ItemDto(Builder builder) {
        this.id = builder.id;
        this.damage = builder.damage;
        this.hasGlint = builder.hasGlint;
        this.color = builder.color;
        this.itemModel = builder.itemModel;
        this.customModelData = builder.customModelData;
        this.trim = builder.trim;
    }

    // --- Getters ---
    public String id() {
        return id;
    }

    public int damage() {
        return damage;
    }

    public boolean hasGlint() {
        return hasGlint;
    }

    public int color() {
        return color;
    }

    public String itemModel() {
        return itemModel;
    }

    public CustomModelDataDto customModelData() {
        return customModelData;
    }

    public TrimDto trim() {
        return trim;
    }

    // --- Builder Pattern ---
    public static class Builder {
        private final String id;
        private int damage = 0;
        private boolean hasGlint = false;
        private int color = -1;
        private String itemModel = "";
        private CustomModelDataDto customModelData = null;
        private TrimDto trim = null;

        public Builder(String id) {
            this.id = id != null ? id : "minecraft:air";
        }

        public Builder damage(int damage) {
            this.damage = damage;
            return this;
        }

        public Builder hasGlint(boolean hasGlint) {
            this.hasGlint = hasGlint;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder itemModel(String itemModel) {
            this.itemModel = itemModel;
            return this;
        }

        public Builder customModelData(CustomModelDataDto customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public Builder trim(TrimDto trim) {
            this.trim = trim;
            return this;
        }

        public ItemDto build() {
            return new ItemDto(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemDto itemDto = (ItemDto) o;
        return damage == itemDto.damage &&
                hasGlint == itemDto.hasGlint &&
                color == itemDto.color &&
                Objects.equals(id, itemDto.id) &&
                Objects.equals(itemModel, itemDto.itemModel) &&
                Objects.equals(customModelData, itemDto.customModelData) &&
                Objects.equals(trim, itemDto.trim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, damage, hasGlint, color, itemModel, customModelData, trim);
    }
}