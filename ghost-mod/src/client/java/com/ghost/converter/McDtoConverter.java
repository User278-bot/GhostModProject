package com.ghost.converter;

import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.mojang.logging.LogUtils;
//? if <= 1.20.1 {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
//?} else {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.CustomModelData;
*///?}
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

    public static HumanoidArm toHumanoidArm(String arm) {
        try {
            return HumanoidArm.valueOf(arm);
        } catch (Exception ex) {
            LogUtils.getLogger().error("Could not convert to HumanoidArm: ", ex);
        }
        return HumanoidArm.RIGHT;
    }

    public static InteractionHand toInteractionHand(String hand) {
        return "OFF_HAND".equals(hand) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
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
                .damage(stack.getDamageValue())
                .hasGlint(stack.hasFoil())
                .color(stack.getBarColor());

        return builder.build();
    }

    public static ItemStack toMc(ItemDto item) {
        if (item == null) {
            return ItemStack.EMPTY;
        } // 1. ID文字列から ResourceLocation を作成

        //? if <= 1.20.6 {
        ResourceLocation location = new ResourceLocation(item.id());
        //?} else if <= 1.21.4 {
        /*ResourceLocation location = ResourceLocation.parse(item.id());
         *///?} else {
        /*Identifier location = Identifier.parse(item.id());
         *///?}


        //? if <= 1.19.2 {
        Item mcItem = Registry.ITEM.get(location);
        //?} else if <= 1.20.6 {
        /*Item mcItem = BuiltInRegistries.ITEM.get(location);
         *///?} else {
        /*Item mcItem = BuiltInRegistries.ITEM.getValue(location);
         *///?}

        ItemStack itemStack = new ItemStack(mcItem);
        itemStack.setDamageValue(item.damage());

        //? if <= 1.20.1 {
        CompoundTag tag = itemStack.getOrCreateTag();
        //?}

        if (item.hasGlint()) {
            //? if <=1.20.1 {

            ListTag enchantmentsList = new ListTag();

            // 空のリストではなく、無害なダミーデータ（例: IDなし、レベル0）を内包したCompoundTagを1つ追加する
            CompoundTag dummyEnchantment = new CompoundTag();
            dummyEnchantment.putString("id", "ghost:dummy");
            dummyEnchantment.putShort("lvl", (short) 0);

            enchantmentsList.add(dummyEnchantment);

            // 1.19.2でのエンチャント情報を管理するバニラ標準のキー "Enchantments" にセット
            tag.put("Enchantments", enchantmentsList);

            //?} else {
            /*itemStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
             *///?}
        }
        if (item.color() != -1) {
            //? if <=1.20.1 {

            CompoundTag display = tag.getCompound("display");
            display.putInt("color", item.color());
            tag.put("display", display);

            //?} else if <=1.21.4 {
            // itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(item.color(), true));
            //?} else {
            /*itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(item.color()));
             *///?}
        }
        if (item.customModelData() != null && !item.customModelData().isEmpty()) {
            //? if <=1.20.1 {
            tag.putInt("CustomModelData", item.customModelData().model());
            //?} else if <= 1.20.6 {
            // itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(item.customModelData().model()));
            //?} else {
            /*itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    item.customModelData().floats(),
                    item.customModelData().flags(),
                    item.customModelData().strings(),
                    item.customModelData().colors())
            );
            *///?}
        }
        // 3. ItemStack を生成
        return itemStack;
    }
}