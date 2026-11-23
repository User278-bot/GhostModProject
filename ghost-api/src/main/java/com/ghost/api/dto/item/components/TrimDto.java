package com.ghost.api.dto.item.components;

/**
 * アーマートリムの見た目を同期するための不変レコード（1.20.1以降対応）。
 */
public record TrimDto(String pattern, String material) {
    public TrimDto {
        pattern = pattern != null ? pattern : "";
        material = material != null ? material : "";
    }
}