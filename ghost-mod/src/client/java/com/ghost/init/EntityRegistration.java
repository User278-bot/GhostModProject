package com.ghost.init;

import com.ghost.api.dto.PlayerData;
import com.ghost.entity.GhostPlayerEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
//? if <=1.20.6 {
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
 
//?}
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=1.21.11 {
/*import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.ResourceLocation;
*///?} else {

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
 
//?}
import net.minecraft.resources.ResourceKey;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.MobCategory;

import java.util.UUID;

/*? >=1.19.3 {*/
/*import net.minecraft.core.registries.BuiltInRegistries;
        *///?}

//? if >=1.21.4 {
/*import net.minecraft.core.registries.Registries;
*///?}

public final class EntityRegistration {
    private EntityRegistration() {
    }

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(EntityRegistration.class);

    public static EntityType<GhostPlayerEntity> GHOST_PLAYER;

    public static void register() {
        LogUtils.getLogger().debug("Registering entity types...");
        GHOST_PLAYER = Registry.register(
        //? if >=1.21.11 {
                /*BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("ghostmod", "ghost_player"),
                EntityType.Builder.<GhostPlayerEntity>of(
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData();
                                    return new GhostPlayerEntity((ClientLevel) world, dummyProfile, dummyData);
                                }, MobCategory.MISC)
                        .sized(0.6f, 1.8f) // プレイヤーと同じサイズ
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("ghostmod", "ghost_player")))
        *///?} else if >=1.21.4 {
                
                /*BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath("ghostmod", "ghost_player"),
                EntityType.Builder.<GhostPlayerEntity>of(
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData();
                                    return new GhostPlayerEntity((ClientLevel) world, dummyProfile, dummyData);
                                }, MobCategory.MISC)
                        .sized(0.6f, 1.8f) // プレイヤーと同じサイズ
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("ghostmod", "ghost_player")))
                
        *///?} else if >=1.20.1 {
                /*BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation("ghostmod", "ghost_player"),
                FabricEntityTypeBuilder.<GhostPlayerEntity>create(
                                MobCategory.MISC,
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData();
                                    return new GhostPlayerEntity((ClientLevel) world, dummyProfile, dummyData);
                                })
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // プレイヤーと同じサイズ
                        .build()
        *///?} else {
                Registry.ENTITY_TYPE,
                new ResourceLocation("ghostmod", "ghost_player"),
                FabricEntityTypeBuilder.<GhostPlayerEntity>create(
                                MobCategory.MISC,
                                (type, world) -> {
                                    GameProfile dummyProfile = new GameProfile(UUID.randomUUID(), "Ghost");
                                    PlayerData dummyData = new PlayerData();
                                    return new GhostPlayerEntity((ClientLevel) world, dummyProfile, dummyData);
                                })
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // プレイヤーと同じサイズ
                        .build()
        //?}
        );

        EntityRendererRegistry.register(
                GHOST_PLAYER,
                (context) -> {
                    // Minecraft標準のPlayerEntityRendererをそのまま使う
                    // "slim" はアレックス（腕が細い）モデルを使うかどうか

                    //? if >=1.21.11 {
                    /*return new AvatarRenderer<>(context, false);
                    *///?} else {
                    return new PlayerRenderer(context, false);
                    //?}
                });
    }
}
