package com.ghost.api.dto.item.components;

import java.util.Objects;

/**
 * アーマートリムの見た目を同期するための不変クラス。（1.20.1以降対応）
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class TrimDto {
    private final String pattern;
    private final String material;

    public TrimDto() {
        this("", "");
    }

    public TrimDto(String pattern, String material) {
        this.pattern = pattern;
        this.material = material;
    }

    public String pattern() { return pattern; }
    public String material() { return material; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrimDto trimDto = (TrimDto) o;
        return Objects.equals(pattern, trimDto.pattern) &&
                Objects.equals(material, trimDto.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, material);
    }
}