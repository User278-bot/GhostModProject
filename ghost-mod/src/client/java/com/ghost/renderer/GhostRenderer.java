package com.ghost.renderer;

import com.ghost.common.dto.PlayerData;
import com.ghost.common.registry.IGhostRegistry;
import com.ghost.entity.GhostPlayerEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GhostRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostRenderer.class);
    private final IGhostRegistry ghostRegistry;
    private static final AtomicInteger nextEntityId = new AtomicInteger(Integer.MIN_VALUE / 2);

    public GhostRenderer(IGhostRegistry ghostRegistry) {
        this.ghostRegistry = ghostRegistry;
    }

    public void onTick(ClientLevel level) {

        if (level == null) return;

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
        for (PlayerData data : latestGhosts) {
            if (existingGhosts.containsKey(data.uuid())) {
                // 既にエンティティが存在する場合 -> 状態を更新
                GhostPlayerEntity ghost = existingGhosts.get(data.uuid());
                ghost.updateFromData(data);

                // 処理済みのゴーストをマップから削除
                existingGhosts.remove(data.uuid());
            } else {
                // エンティティが存在しない場合 -> 新しくスポーン
                GhostPlayerEntity newGhost = new GhostPlayerEntity(level, data);

                // エンティティIDとUUIDをセット
                int uniqueId = nextEntityId.getAndDecrement();
                newGhost.setId(uniqueId); // クライアントサイドエンティティは負のIDを使うのが一般的
                newGhost.setUUID(UUID.fromString(data.uuid()));

                level.addPlayer(newGhost.getId(), newGhost);
            }
        }

        // Registryに存在しなくなったゴーストエンティティをワールドから削除
        for (GhostPlayerEntity ghostToRemove : existingGhosts.values()) {
            ghostToRemove.discard(); // or .remove()
        }
    }

}
