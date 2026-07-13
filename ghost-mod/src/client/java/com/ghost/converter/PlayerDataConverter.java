package com.ghost.converter;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

import static com.ghost.converter.McDtoConverter.fromMc;

public final class PlayerDataConverter {
    private PlayerDataConverter() {
    }

    public static PlayerData fromPlayer(Player player) {
        byte skinParts = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            if (player.isModelPartShown(part)) {
                skinParts |= (byte) part.getMask();
            }
        }

        // 装備情報の抽出
        EquipmentDto equipment = new EquipmentDto(
                fromMc(player.getItemBySlot(EquipmentSlot.MAINHAND)),
                fromMc(player.getItemBySlot(EquipmentSlot.OFFHAND)),
                fromMc(player.getItemBySlot(EquipmentSlot.HEAD)),
                fromMc(player.getItemBySlot(EquipmentSlot.CHEST)),
                fromMc(player.getItemBySlot(EquipmentSlot.LEGS)),
                fromMc(player.getItemBySlot(EquipmentSlot.FEET)));

        return new PlayerData(
                fromMc(player.position()),
                new Vec2Dto(player.getXRot(), player.getYHeadRot()),
                player.getStringUUID(),
                player.getName().getString(),
                player.getPose().toString(),
                //? if >=1.21.11 {
                /* player.level().dimension().identifier().toString(), */
                //?} else if >=1.20.1 {
                 /*player.level().dimension().location().toString(), 
                *///?} else {
                player.level.dimension().location().toString(),
                //?}
                skinParts,
                fromMc(player.getMainArm()),
                player.swingTime,
                equipment);
    }
}