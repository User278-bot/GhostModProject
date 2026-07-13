package com.ghost.converter;

import com.ghost.api.dto.ItemDto;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.mojang.logging.LogUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

//? if <=1.19.2 {
import net.minecraft.core.Registry;
//?} else {
/*import net.minecraft.core.registries.BuiltInRegistries;
 *///?}
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public final class McDtoConverter {
    private McDtoConverter() {
    }

    // --- 既存のメソッド ---
    public static Vec3Dto fromMc(Vec3 vec3) {
        return new Vec3Dto(vec3.x, vec3.y, vec3.z);
    }

    public static String fromMc(InteractionHand hand) {
        return hand != null ? hand.name() : InteractionHand.MAIN_HAND.name();
    }

    public static String fromMc(HumanoidArm hand) {
        return hand != null ? hand.name() : HumanoidArm.RIGHT.name();
    }

    public static Vec2Dto fromMc(Vec2 vec2) {
        return new Vec2Dto(vec2.x, vec2.y);
    }

    public static Vec3 toMc(Vec3Dto v3d) {
        return new Vec3(v3d.x(), v3d.y(), v3d.z());
    }

    public static Vec2 toMc(Vec2Dto v2d) {
        return new Vec2(v2d.x(), v2d.y());
    }

    public static HumanoidArm toMc(String arm) {
        try {
            return HumanoidArm.valueOf(arm);
        } catch (Exception ex) {
            LogUtils.getLogger().error("Could not convert to HumanoidArm: ", ex);
        }
        return HumanoidArm.RIGHT;
    }

    // --- 新規追加: ItemStack変換 ---
    public static ItemDto fromMc(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemDto.ITEM_AIR;
        }

        String id;
        // ? if <=1.19.2 {
        id = Registry.ITEM.getKey(stack.getItem()).toString();
        // ?} else {
        /*
         * id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         */// ?}

        return new ItemDto(id);
    }

    public static ItemStack toMc(ItemDto item) {
        if (item == null) {
            return ItemStack.EMPTY;
        } // 1. ID文字列から ResourceLocation を作成
        ResourceLocation location = new ResourceLocation(item.id()); // もしくは new ResourceLocation("minecraft:stone")
        // 2. Registry から Item を取得
        Item mcItem = Registry.ITEM.get(location);
        // 3. ItemStack を生成
        return new ItemStack(mcItem);
    }
}