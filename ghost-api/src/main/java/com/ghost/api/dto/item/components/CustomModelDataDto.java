package com.ghost.api.dto.item.components;

import java.util.List;
import java.util.Objects;

/**
 * 1.21.2で複合データ化された CustomModelData を表現するための不変クラス。
 * 1.20.6以前の単一int値は、floatsの最初の要素として扱うなどの工夫で後方互換性を保てます。
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class CustomModelDataDto {
    private final List<Float> floats;
    private final List<Boolean> flags;
    private final List<String> strings;
    private final List<Integer> colors;

    public CustomModelDataDto() {
        this(List.of(), List.of(), List.of(), List.of());
    }

    public CustomModelDataDto(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
        this.floats = floats != null ? floats : List.of();
        this.flags = flags != null ? flags : List.of();
        this.strings = strings != null ? strings : List.of();
        this.colors = colors != null ? colors : List.of();
    }

    public List<Float> floats() {
        return floats;
    }

    public List<Boolean> flags() {
        return flags;
    }

    public List<String> strings() {
        return strings;
    }

    public List<Integer> colors() {
        return colors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomModelDataDto that = (CustomModelDataDto) o;
        return Objects.equals(floats, that.floats) &&
                Objects.equals(flags, that.flags) &&
                Objects.equals(strings, that.strings) &&
                Objects.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(floats, flags, strings, colors);
    }
}