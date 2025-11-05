package com.ghost.entity;

import com.ghost.common.dto.PlayerData;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Pose;

import java.util.UUID;

// Playerエンティティを継承すると多くの機能を使えるが、
// 描画だけなら、より軽量な Entity を継承する方がシンプルな場合もある。
// ここではPlayerを継承して、見た目の互換性を高める。
public class GhostPlayerEntity extends RemotePlayer {

    // このゴーストのUUIDを保持する
    private final String ghostUuid;

    // Playerを継承するために、ダミーの引数を渡す必要がある
    public GhostPlayerEntity(ClientLevel world, PlayerData data) {
        super(
                world,
                new GameProfile(UUID.fromString(data.uuid()), data.name()),
                null
        );
        this.ghostUuid = data.uuid();
        updateFromData(data);
    }

    // GhostRegistryから受け取った最新のデータで、エンティティの状態を更新するメソッド
    public void updateFromData(final PlayerData data) {
        final int interpolationSteps = 4;

        // ★ Minecraftの滑らかな移動メソッドを呼び出す
        this.lerpTo(
                data.pos().x(),
                data.pos().y(),
                data.pos().z(),
                data.rot().y(), // ★ YawはY軸周りの回転
                data.rot().x(), // ★ PitchはX軸周りの回転
                interpolationSteps,
                false // テレポートはしない
        );
        this.lerpHeadTo(data.rot().y(), interpolationSteps);
        try {
            Pose newPose = Pose.valueOf(data.pose());
            this.setPose(newPose);
        } catch (IllegalArgumentException e) {
            // 不正なポーズ名が送られてきた場合は無視する
        }

    }

    public String getGhostUuid() {
        return this.ghostUuid;
    }

    // このエンティティはクライアントサイドのみの幻影なので、
    // 物理演算やAIのtick処理は一切行わないようにする
    @Override
    public void tick() {
        // 何もしない
        super.tick();
        //this.calculateEntityAnimation(this, false);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return true; // isCreativeでないと腕や足が揺れないことがある
    }
}