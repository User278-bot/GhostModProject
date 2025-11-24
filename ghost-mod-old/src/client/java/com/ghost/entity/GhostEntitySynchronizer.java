package com.ghost.entity;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.registry.IGhostRegistry;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GhostEntitySynchronizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostEntitySynchronizer.class);
    private final IGhostRegistry ghostRegistry;
    private static final AtomicInteger nextEntityId = new AtomicInteger(Integer.MIN_VALUE / 2);

    public GhostEntitySynchronizer(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = ghostRegistry;
    }

    private void updateGhosts(ClientLevel level, Map<String, GhostPlayerEntity> existingGhosts,
            Collection<PlayerData> latestGhosts) {
        // LOGGER.info("Updating ghosts: existing={}, latest={}", existingGhosts.size(),
        // latestGhosts.size());
        for (PlayerData data : latestGhosts) {
            if (existingGhosts.containsKey(data.uuid())) {
                // 既にエンティティが存在する場合 -> 状態を更新
                GhostPlayerEntity ghost = existingGhosts.get(data.uuid());
                ghost.updateFromData(data);

                // 処理済みのゴーストをマップから削除
                existingGhosts.remove(data.uuid());
            } else {
                // エンティティが存在しない場合 -> 新しくスポーン
                LOGGER.debug("Spawning new ghost: uuid={}, name={}", data.uuid(), data.name());

                // スキン情報の非同期取得を開始 (Futureを作成)
                var skinFuture = fetchSkinLocation(data.uuid(), data.name());

                GhostPlayerEntity newGhost = new GhostPlayerEntity(level,
                        new GameProfile(UUID.fromString(data.uuid()), data.name()), data, skinFuture);

                // エンティティIDとUUIDをセット
                int uniqueId = nextEntityId.getAndDecrement();
                newGhost.setId(uniqueId); // クライアントサイドエンティティは負のIDを使うのが一般的

                // UUIDはコンストラクタでGameProfile経由でセットされるが、念のため確認
                // newGhost.setUUID(UUID.fromString(data.uuid()));

                level.addPlayer(newGhost.getId(), newGhost);
                LOGGER.debug("Added ghost to level: id={}, uuid={}", newGhost.getId(), newGhost.getUUID());
            }
        }
    }

    private void removeGhosts(Map<String, GhostPlayerEntity> ghostsToRemove) {
        for (GhostPlayerEntity ghostToRemove : ghostsToRemove.values()) {
            LOGGER.debug("Removing ghost: uuid={}", ghostToRemove.getGhostUuid());
            ghostToRemove.discard(); // or .remove()
        }
    }

    public void onTick(ClientLevel level) {
        if (level == null)
            return;

        // 現在ワールドにいるゴーストエンティティを一旦すべて集める
        Map<String, GhostPlayerEntity> existingGhosts = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof GhostPlayerEntity) {
                existingGhosts.put(((GhostPlayerEntity) entity).getGhostUuid(), (GhostPlayerEntity) entity);
            }
        }

        // GhostRegistryから最新のゴースト情報を取得
        Collection<PlayerData> latestGhosts = ghostRegistry.getAllGhosts();

        // Registryの情報を元に、エンティティを更新または新規スポーン
        updateGhosts(level, existingGhosts, latestGhosts);

        // Registryに存在しなくなったゴーストエンティティをワールドから削除
        removeGhosts(existingGhosts);
    }

    public CompletableFuture<ResourceLocation> fetchSkinLocation(String uuidString, String name) {
        if (uuidString == null || uuidString.isEmpty())
            return CompletableFuture.completedFuture(null);

        // 非同期でスキン情報を取得
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = UUID.fromString(uuidString);
                // セッションサービスを使ってプロファイル情報を埋める（通信発生）
                GameProfile profile = new GameProfile(uuid, name);
                GameProfile updatedProfile = Minecraft.getInstance().getMinecraftSessionService()
                        .fillProfileProperties(profile, true);

                if (updatedProfile != null) {
                    CompletableFuture<ResourceLocation> textureFuture = new CompletableFuture<>();

                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().getSkinManager().registerSkins(updatedProfile,
                                (type, location, profile1) -> {
                                    if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN) {
                                        textureFuture.complete(location);
                                    }
                                }, true);
                    });

                    // タイムアウトなどを考慮すべきだが、今回は簡易実装
                    return textureFuture.join();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load skin for ghost: {}", name, e);
            }
            return null;
        });
    }
}
