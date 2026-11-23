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
    /** 前回の tick で同期対象ゴーストが存在していたかどうか（0 体定常時の走査スキップ用） */
    private boolean hadGhostsLastTick = false;

    public GhostEntitySynchronizer(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = ghostRegistry;
    }

    private void updateGhosts(ClientLevel level, Map<String, GhostPlayerEntity> existingGhosts,
                              Collection<PlayerData> latestGhosts) {
        for (PlayerData data : latestGhosts) {
            GhostPlayerEntity existing = existingGhosts.remove(data.uuid());
            if (existing != null) {
                // 既にエンティティが存在する場合は状態を更新
                existing.updateFromData(data);
            } else {
                // エンティティが存在しない場合は新しくスポーン
                UUID parsedUuid;
                try {
                    parsedUuid = UUID.fromString(data.uuid());
                } catch (IllegalArgumentException e) {
                    LogUtils.getLogger().warn("不正な UUID のゴーストデータをスキップしました: {}", data.uuid());
                    continue;
                }

                LogUtils.getLogger().debug("Spawning new ghost: uuid={}, name={}", data.uuid(), data.name());

                var newGameProfile = new GameProfile(parsedUuid, data.name());
                GhostPlayerEntity newGhost = new GhostPlayerEntity(level, newGameProfile, data);

                // クライアントサイド専用エンティティのため負の ID を割り当て
                int uniqueId = nextEntityId.getAndDecrement();
                newGhost.setId(uniqueId);

                //? if >=1.20.6 {
                /*level.addEntity(newGhost);
                 *///?} else {
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
            ghostToRemove.discard();
        }
    }

    public void onTick(ClientLevel level) {
        if (level == null)
            return;

        // GhostRegistry から最新のゴースト情報を取得
        Collection<PlayerData> latestGhosts = ghostRegistry.getAllGhosts();

        // 同期対象が 0 体かつ前回の tick でも 0 体だった場合は、全エンティティ走査をスキップ
        if (latestGhosts.isEmpty() && !hadGhostsLastTick) {
            return;
        }

        // 現在ワールドにいるゴーストエンティティを収集
        Map<String, GhostPlayerEntity> existingGhosts = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof GhostPlayerEntity ghost) {
                existingGhosts.put(ghost.getGhostUuid(), ghost);
            }
        }

        // Registry の情報を元に、エンティティを更新または新規スポーン
        updateGhosts(level, existingGhosts, latestGhosts);

        // Registry に存在しなくなった（または距離外判定された）ゴーストエンティティをワールドから削除
        removeGhosts(existingGhosts);

        hadGhostsLastTick = !latestGhosts.isEmpty();
    }
}
