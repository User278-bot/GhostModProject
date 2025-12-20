package com.ghost.init;

import com.ghost.api.dto.PlayerData;
import com.ghost.entity.GhostPlayerEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.MobCategory;

import java.util.UUID;

/*? >=1.19.3 {*/
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?}

public final class EntityRegistration {
    private EntityRegistration() {
    }

    //private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistration.class);

    public static EntityType<GhostPlayerEntity> GHOST_PLAYER;

    public static void register() {
        LogUtils.getLogger().info("Registering entity types...");
        GHOST_PLAYER = Registry.register(
                /*? >=1.19.3 {*/
                /*BuiltInRegistries.ENTITY_TYPE,
                *//*?} else {*/
                Registry.ENTITY_TYPE,
                 //?}
                new ResourceLocation("ghostmod", "ghost_player"),
                FabricEntityTypeBuilder.<GhostPlayerEntity>create(
                                MobCategory.MISC,
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData();

                                    return new GhostPlayerEntity((ClientLevel) world, dummyProfile, dummyData, null);
                                })
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // プレイヤーと同じサイズ
                        .build());
        EntityRendererRegistry.register(
                GHOST_PLAYER,
                (context) -> {
                    // Minecraft標準のPlayerEntityRendererをそのまま使う
                    // "slim" はアレックス（腕が細い）モデルを使うかどうか
                    return new PlayerRenderer(context, false);
                });

    }
}
