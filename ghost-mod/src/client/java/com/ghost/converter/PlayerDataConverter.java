package com.ghost.converter;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
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

        return new PlayerData(
                fromMc(player.position()),
                new Vec2Dto(player.getXRot(), player.getYHeadRot()),
                player.getStringUUID(),
                player.getName().getString(),
                player.getPose().toString(),
                //? if >=1.21.11 {
                /*player.level().dimension().identifier().toString(),
                *//*?} else if >=1.20.1 {*/
                /*player.level().dimension().location().toString(),*/
                 /*?} else {*/
                player.level.dimension().location().toString(),
                //?}
                skinParts,
                fromMc(player.getMainArm()),
                player.swingTime);
    }
}
