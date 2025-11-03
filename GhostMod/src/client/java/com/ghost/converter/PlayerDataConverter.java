package com.ghost.converter;

import com.ghost.common.dto.PlayerData;
import net.minecraft.world.entity.player.Player;

import static com.ghost.converter.McDtoConverter.fromMc;

public final class PlayerDataConverter {
    private PlayerDataConverter() {
    }

    public static PlayerData fromPlayer(Player player) {
        return new PlayerData(
                fromMc(player.position()),
                fromMc(player.getRotationVector()),
                player.getStringUUID(),
                player.getName().getString(),
                player.getPose().toString(),
                player.level.dimension().location().toString()
        );
    }
}
