package com.ghost.init;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.dto.Vec2Dto;
import com.ghost.common.dto.Vec3Dto;
import com.ghost.entity.GhostPlayerEntity;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class EntityRegistration {
    private EntityRegistration() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistration.class);

    public static EntityType<GhostPlayerEntity> GHOST_PLAYER;

    public static void register() {
        LOGGER.info("Registering entity types...");
        GHOST_PLAYER = Registry.register(
                Registry.ENTITY_TYPE,
                new ResourceLocation("ghostmod", "ghost_player"),
                FabricEntityTypeBuilder.<GhostPlayerEntity>create(
                                MobCategory.MISC,
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData(
                                            Vec3Dto.ZERO,
                                            Vec2Dto.ZERO,
                                            "0000",
                                            "dummy",
                                            "DUMMY",
                                            "dummy:dimension"
                                    );

                                    return new GhostPlayerEntity((ClientLevel) world, dummyData);
                                })
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // プレイヤーと同じサイズ
                        .build()
        );
        EntityRendererRegistry.register(
                GHOST_PLAYER,
                (context) -> {
                    // Minecraft標準のPlayerEntityRendererをそのまま使う
                    // "slim" はアレックス（腕が細い）モデルを使うかどうか
                    return new PlayerRenderer(context, false);
                }
        );

    }
}
