package com.ghost.api.dto.item.components;

import java.util.List;

/**
 * 1.21.2で複合データ化された CustomModelData を表現するための不変データレコード。
 * 1.20.6以前の単一 int 値は model フィールドで保持します。
 */
public record CustomModelDataDto(
        List<Float> floats,
        List<Boolean> flags,
        List<String> strings,
        List<Integer> colors,
        int model
) {
    public CustomModelDataDto {
        floats = floats != null ? List.copyOf(floats) : List.of();
        flags = flags != null ? List.copyOf(flags) : List.of();
        strings = strings != null ? List.copyOf(strings) : List.of();
        colors = colors != null ? List.copyOf(colors) : List.of();
    }

    public CustomModelDataDto() {
        this(List.of(), List.of(), List.of(), List.of(), 0);
    }

    public CustomModelDataDto(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
        this(floats, flags, strings, colors, 0);
    }

    public CustomModelDataDto(int model) {
        this(List.of(), List.of(), List.of(), List.of(), model);
    }

    /**
     * すべての複合データが空かつ単一モデル値も未設定（0）である場合に true を返します。
     */
    public boolean isEmpty() {
        return floats.isEmpty()
                && flags.isEmpty()
                && strings.isEmpty()
                && colors.isEmpty()
                && model == 0;
    }
}