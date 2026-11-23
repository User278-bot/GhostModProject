package com.ghost.api.dto;

import com.ghost.api.dto.item.components.CustomModelDataDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CustomModelDataDto} の空判定・等価性・不変性を検証するユニットテスト。
 */
class CustomModelDataDtoTest {

    @Test
    void isEmpty_AllEmptyAndZeroModel_ReturnsTrue() {
        assertTrue(new CustomModelDataDto().isEmpty());
        assertTrue(new CustomModelDataDto(List.of(), List.of(), List.of(), List.of()).isEmpty());
        assertTrue(new CustomModelDataDto(List.of(), List.of(), List.of(), List.of(), 0).isEmpty());
        assertTrue(new CustomModelDataDto(null, null, null, null, 0).isEmpty());
    }

    @Test
    void isEmpty_AnyFieldNonEmptyOrModelNonZero_ReturnsFalse() {
        assertFalse(new CustomModelDataDto(List.of(1.5f), List.of(), List.of(), List.of(), 0).isEmpty());
        assertFalse(new CustomModelDataDto(List.of(), List.of(true), List.of(), List.of(), 0).isEmpty());
        assertFalse(new CustomModelDataDto(List.of(), List.of(), List.of("a"), List.of(), 0).isEmpty());
        assertFalse(new CustomModelDataDto(List.of(), List.of(), List.of(), List.of(0xFF0000), 0).isEmpty());
        assertFalse(new CustomModelDataDto(42).isEmpty());
    }

    @Test
    void equalsAndHashCode_IncludeModelField() {
        CustomModelDataDto cmd1 = new CustomModelDataDto(100);
        CustomModelDataDto cmd2 = new CustomModelDataDto(100);
        CustomModelDataDto cmd3 = new CustomModelDataDto(200);

        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
        assertNotEquals(cmd1, cmd3);
    }

    @Test
    void listsAreDefensivelyCopiedAndImmutable() {
        List<Float> mutableFloats = new ArrayList<>(List.of(1.0f));
        List<Boolean> mutableFlags = new ArrayList<>(List.of(true));
        List<String> mutableStrings = new ArrayList<>(List.of("v1"));
        List<Integer> mutableColors = new ArrayList<>(List.of(0x112233));

        CustomModelDataDto dto = new CustomModelDataDto(
                mutableFloats, mutableFlags, mutableStrings, mutableColors, 7);

        // 元のミュータブルリストを変更しても DTO に影響しないこと
        mutableFloats.add(2.0f);
        mutableFlags.add(false);
        mutableStrings.add("v2");
        mutableColors.add(0x445566);

        assertEquals(List.of(1.0f), dto.floats());
        assertEquals(List.of(true), dto.flags());
        assertEquals(List.of("v1"), dto.strings());
        assertEquals(List.of(0x112233), dto.colors());

        // DTO が返すリストも変更不可であること
        assertThrows(UnsupportedOperationException.class, () -> dto.floats().add(3.0f));
        assertThrows(UnsupportedOperationException.class, () -> dto.flags().add(true));
        assertThrows(UnsupportedOperationException.class, () -> dto.strings().add("v3"));
        assertThrows(UnsupportedOperationException.class, () -> dto.colors().add(0));
    }
}
