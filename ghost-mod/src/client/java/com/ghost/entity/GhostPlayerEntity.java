package com.ghost.entity;

import com.ghost.api.dto.PlayerData;
import com.ghost.converter.McDtoConverter;
import com.mojang.authlib.GameProfile;
//? if >=1.21.11 {
//?} else {
import net.minecraft.MethodsReturnNonnullByDefault;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.player.PlayerSkin;
 *//*?} else if >=1.20.6 {*/
 /*import net.minecraft.client.resources.PlayerSkin;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Playerエンティティを継承すると多くの機能を使えるが、
// 描画だけなら、より軽量な Entity を継承する方がシンプルな場合もある。
// ここではPlayerを継承して、見た目の互換性を高める。
public class GhostPlayerEntity extends RemotePlayer {

    // このゴーストのUUIDを保持する
    private final String ghostUuid;
    private static final int INTERPOLATION_STEPS = 4;

    public GhostPlayerEntity(final ClientLevel world,
                             final GameProfile profile,
                             final PlayerData data) {
        /*? >=1.20.1 {*/
        /*super(world, profile);
         *//*?} else {*/
        super(world, profile, null);
        //?}
        this.ghostUuid = data.uuid();

        // 初期座標を確定させる
        this.setPos(data.pos().x(), data.pos().y(), data.pos().z());
        this.setRot(data.rot().y(), data.rot().x());
        this.setYHeadRot(data.rot().y());

        // 状態同期
        syncState(data);

        // スキン情報の非同期取得
        //? if >=1.21.11 {
        /*CompletableFuture.runAsync(() -> {
            var updatedProfile = Minecraft.getInstance().services().sessionService().fetchProfile(profile.id(), true);
            var skinSupplier = Minecraft.getInstance().getSkinManager().get(Objects.requireNonNull(updatedProfile).profile());

            skinSupplier.thenAccept((playerSkin) -> {
                playerSkin.ifPresent(skin -> this.skinLocation = skin);
            });
        });
        *///?} else if >=1.21.4 {
        
        /*CompletableFuture.runAsync(() -> {
            var updatedProfile = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.getId(), true);
            var skinSupplier = Minecraft.getInstance().getSkinManager().getOrLoad(Objects.requireNonNull(updatedProfile).profile());

            skinSupplier.thenAccept((playerSkin) -> {
                playerSkin.ifPresent(skin -> this.skinLocation = skin);
            });
        });
        
        *//*?} else if >=1.20.6 {*/
        
        /*CompletableFuture.runAsync(() -> {
            var updatedProfile = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.getId(), true);
            var skinSupplier = Minecraft.getInstance().getSkinManager().getOrLoad(Objects.requireNonNull(updatedProfile).profile());

            skinSupplier.thenAccept((playerSkin) -> this.skinLocation = playerSkin);
        });
        *//*?} else {*/

        CompletableFuture.supplyAsync(() -> {
            try {
                GameProfile filledProfile = Minecraft.getInstance().getMinecraftSessionService()
                        .fillProfileProperties(profile, true);
                if (filledProfile == null) return null;

                CompletableFuture<net.minecraft.resources.ResourceLocation> future = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSkinManager().registerSkins(filledProfile, (type, location, p1) -> {
                    if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN) {
                        future.complete(location);
                    }
                }, true));
                return future.join();
            } catch (Exception e) {
                return null;
            }
        }).thenAcceptAsync(location -> {
            if (location != null) this.skinLocation = location;
        }, Minecraft.getInstance());
        //?}
    }


    // GhostRegistryから受け取った最新のデータで、エンティティの状態を更新するメソッド
    public void updateFromData(final PlayerData data) {
        if (data == null) {
            return;
        }

        // ★ Minecraftの滑らかな移動メソッドを呼び出す
        //? if >= 1.21.11 {
        /*this.lerpPositionAndRotationStep(
                1,
                data.pos().x(),
                data.pos().y(),
                data.pos().z(),
                data.rot().y(), // ★ YawはY軸周りの回転
                data.rot().x()  // ★ PitchはX軸周りの回転
        );
        *///?} else if >= 1.20.6 {
        
        /*this.lerpTo(
                data.pos().x(),
                data.pos().y(),
                data.pos().z(),
                data.rot().y(), // ★ YawはY軸周りの回転
                data.rot().x(), // ★ PitchはX軸周りの回転
                INTERPOLATION_STEPS
        );
        
        *///?} else {

        this.lerpTo(
                data.pos().x(),
                data.pos().y(),
                data.pos().z(),
                data.rot().y(), // ★ YawはY軸周りの回転
                data.rot().x(), // ★ PitchはX軸周りの回転
                INTERPOLATION_STEPS,
                false
        );

        //?}
        this.lerpHeadTo(data.rot().y(), INTERPOLATION_STEPS);

        syncState(data);
    }

    private void syncState(PlayerData data) {
        if (data.swingTime() == 1) {
            this.setMainArm(McDtoConverter.toMc(data.swingArm()));
            this.swing(InteractionHand.MAIN_HAND);
        }

        try {
            Pose newPose = Pose.valueOf(data.pose());
            this.setPose(newPose);
        } catch (IllegalArgumentException e) {
            // 不正なポーズ名が送られてきた場合は無視する
        }
        // Skin Parts Synchronization
        this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, data.skinParts());

        this.setItemSlot(EquipmentSlot.MAINHAND, McDtoConverter.toMc(data.equipment().mainHand()));
        this.setItemSlot(EquipmentSlot.OFFHAND, McDtoConverter.toMc(data.equipment().offHand()));
        this.setItemSlot(EquipmentSlot.FEET, McDtoConverter.toMc(data.equipment().feet()));
        this.setItemSlot(EquipmentSlot.CHEST, McDtoConverter.toMc(data.equipment().chest()));
        this.setItemSlot(EquipmentSlot.HEAD, McDtoConverter.toMc(data.equipment().head()));
        this.setItemSlot(EquipmentSlot.LEGS, McDtoConverter.toMc(data.equipment().legs()));
    }

    public String getGhostUuid() {
        return this.ghostUuid;
    }

    // このエンティティはクライアントサイドのみの幻影なので、
    // 物理演算やAIのtick処理は一切行わないようにする
    @Override
    public void tick() {
        super.tick();
        // this.calculateEntityAnimation(this, false);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return true; // isCreativeでないと腕や足が揺れないことがある
    }
    // --- 当たり判定と物理挙動の無効化 ---

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        // 何もしない
    }


    /*? if >=1.20.6 {*/

    /*private volatile PlayerSkin skinLocation = null;

    @Override
    public @NotNull PlayerSkin getSkin() {
        return skinLocation != null ? skinLocation : super.getSkin();
    }

    *//*?} else {*/
    // --- Skin Handling ---
    private volatile net.minecraft.resources.ResourceLocation skinLocation = null;

    @Override
    @MethodsReturnNonnullByDefault
    public net.minecraft.resources.ResourceLocation getSkinTextureLocation() {
        return skinLocation != null ? skinLocation : super.getSkinTextureLocation();
    }
    //?}
}