package com.ghost.entity;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.registry.IGhostRegistry;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GhostEntitySynchronizer {
    private final IGhostRegistry ghostRegistry;
    private static final AtomicInteger nextEntityId = new AtomicInteger(Integer.MIN_VALUE / 2);

    public GhostEntitySynchronizer(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = ghostRegistry;
    }

    private void updateGhosts(ClientLevel level, Map<String, GhostPlayerEntity> existingGhosts,
                              Collection<PlayerData> latestGhosts) {
        for (PlayerData data : latestGhosts) {
            if (existingGhosts.containsKey(data.uuid())) {
                // 既にエンティティが存在する場合 -> 状態を更新
                GhostPlayerEntity ghost = existingGhosts.get(data.uuid());
                ghost.updateFromData(data);

                // 処理済みのゴーストをマップから削除
                existingGhosts.remove(data.uuid());
            } else {
                // エンティティが存在しない場合 -> 新しくスポーン
                LogUtils.getLogger().debug("Spawning new ghost: uuid={}, name={}", data.uuid(), data.name());

                var newGameProfile = new GameProfile(UUID.fromString(data.uuid()), data.name());
                GhostPlayerEntity newGhost = new GhostPlayerEntity(level, newGameProfile, data);

                // エンティティIDとUUIDをセット
                int uniqueId = nextEntityId.getAndDecrement();
                newGhost.setId(uniqueId); // クライアントサイドエンティティは負のIDを使うのが一般的

                /*? >=1.20.6 {*/
                /*level.addEntity(newGhost);
                 *//*?} else {*/
                level.addPlayer(newGhost.getId(), newGhost);
                //?}
                LogUtils.getLogger().debug("Added ghost to level: id={}, uuid={}", newGhost.getId(),
                        newGhost.getUUID());
            }
        }
    }

    private void removeGhosts(Map<String, GhostPlayerEntity> ghostsToRemove) {
        for (GhostPlayerEntity ghostToRemove : ghostsToRemove.values()) {
            LogUtils.getLogger().debug("Removing ghost: uuid={}", ghostToRemove.getGhostUuid());
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

        // Registryに存在しなくなった（または距離外判定された）ゴーストエンティティをワールドから削除
        removeGhosts(existingGhosts);
    }
}
