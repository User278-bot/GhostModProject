package com.ghost.converter;

import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

//? if <=1.19.2 {
import net.minecraft.core.Registry;
        //?} else {
/*import net.minecraft.core.registries.BuiltInRegistries;
 *///?}

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
 *///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

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
        //? if <=1.19.2 {
        id = Registry.ITEM.getKey(stack.getItem()).toString();
        //?} else {
        /*id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         *///?}

        ItemDto.Builder builder = new ItemDto.Builder(id)
                //? if<=1.19.2{
                .damage(stack.getDamageValue())
                .hasGlint(stack.hasFoil())
                .color(stack.getBarColor());
        //? }else if<=1.20.1{
        //? }else if<=1.20.6{
        //? }else if<=1.20.11{
        //? }else{
        //? }

        return builder.build();
    }

    public static ItemStack toMc(ItemDto item) {
        if (item == null) {
            return ItemStack.EMPTY;
        } // 1. ID文字列から ResourceLocation を作成

        //? if <= 1.20.6 {
        ResourceLocation location = new ResourceLocation(item.id());
        //?} else if <= 1.21.4 {
        // ResourceLocation location = ResourceLocation.parse(item.id());
        //?} else {
        /*Identifier location = Identifier.parse(item.id());
         *///?}

        ItemStack itemStack;
        //? if <= 1.19.2 {
        Item mcItem = Registry.ITEM.get(location);
        itemStack = new ItemStack(mcItem);
        itemStack.setDamageValue(item.damage());
        CompoundTag tag = itemStack.getOrCreateTag();
        if (item.hasGlint()) {

            ListTag enchantmentsList = new ListTag();

            // 空のリストではなく、無害なダミーデータ（例: IDなし、レベル0）を内包したCompoundTagを1つ追加する
            CompoundTag dummyEnchantment = new CompoundTag();
            dummyEnchantment.putString("id", "ghost:dummy");
            dummyEnchantment.putShort("lvl", (short) 0);

            enchantmentsList.add(dummyEnchantment);

            // 1.19.2でのエンチャント情報を管理するバニラ標準のキー "Enchantments" にセット
            tag.put("Enchantments", enchantmentsList);
        }
        if (item.color() != -1) {

            CompoundTag display = tag.getCompound("display");
            display.putInt("color", item.color());
            tag.put("display", display);
        }

        //?} else if <= 1.20.6 {
        /*Item mcItem = BuiltInRegistries.ITEM.get(location);
         *///?} else if <= 1.21.4 {
        // Item mcItem = BuiltInRegistries.ITEM.getValue(location);
        //?} else {
        /*Item mcItem = BuiltInRegistries.ITEM.getValue(location);
         *///?}

        // 3. ItemStack を生成
        return itemStack;
    }
}