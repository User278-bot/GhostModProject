package com.ghost.converter;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.dto.Vec2Dto;
import net.minecraft.world.entity.player.Player;

import static com.ghost.converter.McDtoConverter.fromMc;

public final class PlayerDataConverter {
    private PlayerDataConverter() {
    }

    public static PlayerData fromPlayer(Player player) {
        return new PlayerData(
                fromMc(player.position()),
                new Vec2Dto(player.getXRot(), player.getYHeadRot()),
                player.getStringUUID(),
                player.getName().getString(),
                player.getPose().toString(),
                player.level.dimension().location().toString()
        );
    }
}
