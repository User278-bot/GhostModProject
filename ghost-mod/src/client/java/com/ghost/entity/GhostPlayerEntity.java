package com.ghost.entity;

import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
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
//? if >=26.3 {
/*import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
*///?}
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
//? if >= 1.21.11 {
/*import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
*///? }
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Playerエンティティを継承すると多くの機能を使えるが、
// 描画だけなら、より軽量な Entity を継承する方がシンプルな場合もある。
// ここではPlayerを継承して、見た目の互換性を高める。
public class GhostPlayerEntity extends RemotePlayer {

    // このゴーストのUUIDを保持する
    private final String ghostUuid;
    // ネットワーク遅延・ジッタ吸収のため補間ステップ数を4 tickに統一
    private static final int INTERPOLATION_STEPS = 4;
    // テレポート等の急激な移動時に速度推定をリセットする距離の二乗閾値（10ブロック）
    private static final double MAX_VELOCITY_DISTANCE_SQR = 100.0;

    // 不要な補間リセットを防ぐため、最後に適用した状態を保持する
    private PlayerData lastAppliedData;
    private Vec3Dto lastSyncedPos;
    private Vec2Dto lastSyncedRot;

    public GhostPlayerEntity(final ClientLevel world,
                             final GameProfile profile,
                             final PlayerData data) {
        /*? >=1.20.1 {*/

        /*super(world, profile);
        *//*?} else {*/
        super(world, profile, null);
         //?}
        this.ghostUuid = data.uuid();

        //? if >=26.3 {
        /*// 26.3 以降は createInterpolationHandler() で LinearInterpolationHandler(INTERPOLATION_STEPS) を生成済み
        *///?} elif >=1.21.11 {
        /*// 位置・体回転の補間長を頭回転（INTERPOLATION_STEPS）と揃える
        this.getInterpolation().setInterpolationLength(INTERPOLATION_STEPS);
        *///?}

        // 初期座標・回転と前 tick 状態を確定させ、スポーン直後の飛び・回転を防ぐ
        this.setPos(data.pos().x(), data.pos().y(), data.pos().z());
        this.setRot(data.rot().y(), data.rot().x());
        this.setYHeadRot(data.rot().y());
        this.yBodyRot = data.rot().y();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setOldPosAndRot();

        // 状態同期
        syncState(data);
        this.lastAppliedData = data;
        this.lastSyncedPos = data.pos();
        this.lastSyncedRot = data.rot();

        // スキン情報の非同期取得
        //? if >=1.21.11 {

        /*CompletableFuture.runAsync(() -> {
            var updatedProfile =
                    Minecraft.getInstance().services().sessionService().fetchProfile(profile.id()
                            , true);
            var skinSupplier =
                    Minecraft.getInstance().getSkinManager().get(Objects.requireNonNull(
                            updatedProfile).profile());

            skinSupplier.thenAccept((playerSkin) -> {
                playerSkin.ifPresent(skin -> this.skinLocation = skin);
            });
        });
        *///?} else if >=1.21.4 {

        
          /*CompletableFuture.runAsync(() -> {
          var updatedProfile =
          Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.
          getId(), true);
          var skinSupplier =
          Minecraft.getInstance().getSkinManager().getOrLoad(Objects.requireNonNull(
          updatedProfile).profile());

          skinSupplier.thenAccept((playerSkin) -> {
          playerSkin.ifPresent(skin -> this.skinLocation = skin);
          });
          });

         *//*?} else if >=1.20.6 {*/


          /*CompletableFuture.runAsync(() -> {
          var updatedProfile =
          Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.
          getId(), true);
          var skinSupplier =
          Minecraft.getInstance().getSkinManager().getOrLoad(Objects.requireNonNull(
          updatedProfile).profile());

          skinSupplier.thenAccept((playerSkin) -> this.skinLocation = playerSkin);
          });
         *//*?} else {*/

        CompletableFuture.supplyAsync(() -> {
            try {
                GameProfile filledProfile = Minecraft.getInstance().getMinecraftSessionService()
                        .fillProfileProperties(profile, true);
                if (filledProfile == null)
                    return null;

                CompletableFuture<net.minecraft.resources.ResourceLocation> future = new CompletableFuture<>();
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSkinManager()
                        .registerSkins(filledProfile, (type, location, p1) -> {
                            if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN) {
                                future.complete(location);
                            }
                        }, true));
                return future.join();
            } catch (Exception e) {
                return null;
            }
        }).thenAcceptAsync(location -> {
            if (location != null)
                this.skinLocation = location;
        }, Minecraft.getInstance());
        //?}
    }

    // GhostRegistryから受け取った最新のデータで、エンティティの状態を更新するメソッド
    public void updateFromData(final PlayerData data) {
        if (data == null || data.pos() == null || data.rot() == null
                || Objects.equals(this.lastAppliedData, data)) {
            return;
        }

        Vec3Dto newPos = data.pos();
        Vec2Dto newRot = data.rot();
        boolean posChanged = !Objects.equals(this.lastSyncedPos, newPos);
        boolean rotChanged = !Objects.equals(this.lastSyncedRot, newRot);
        boolean headYawChanged = this.lastSyncedRot == null
                || Float.compare(this.lastSyncedRot.y(), newRot.y()) != 0;

        if (posChanged || rotChanged) {
            // ★ Minecraftの滑らかな移動メソッドを呼び出す
            //? if >= 1.21.11 {
            /*Vec3 pos = McDtoConverter.toMc(newPos);
            this.moveOrInterpolateTo(pos, newRot.y(), newRot.x());
            *///?} elif >= 1.20.6 {
            /*this.lerpTo(
                    newPos.x(),
                    newPos.y(),
                    newPos.z(),
                    newRot.y(), // ★ YawはY軸周りの回転
                    newRot.x(), // ★ PitchはX軸周りの回転
                    INTERPOLATION_STEPS
            );
            *///?} else {
            this.lerpTo(
                    newPos.x(),
                    newPos.y(),
                    newPos.z(),
                    newRot.y(), // ★ YawはY軸周りの回転
                    newRot.x(), // ★ PitchはX軸周りの回転
                    INTERPOLATION_STEPS,
                    false);
            //?}
        }

        if (headYawChanged) {
            this.lerpHeadTo(newRot.y(), INTERPOLATION_STEPS);
        }

        syncState(data);

        this.lastAppliedData = data;
        this.lastSyncedPos = newPos;
        this.lastSyncedRot = newRot;
    }

    private void syncState(PlayerData data) {
        this.setMainArm(McDtoConverter.toHumanoidArm(data.mainArm()));
        if (data.swingTime() == 1) {
            //? if >=26.3 {
            /*this.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            *///? }else{
            this.swing(InteractionHand.MAIN_HAND);
            //?}
        }

        if (data.pose() != null) {
            try {
                Pose newPose = Pose.valueOf(data.pose());
                this.setPose(newPose);
                // スニーク時のネームタグ挙動（壁越し非表示・半透明化・表示距離32ブロック制限）をバニラに合わせる
                this.setShiftKeyDown(newPose == Pose.CROUCHING);
            } catch (IllegalArgumentException e) {
                // 不正なポーズ名が送られてきた場合は無視する
            }
        }
        // Skin Parts Synchronization
        this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, data.skinParts());

        this.setItemSlot(EquipmentSlot.MAINHAND, McDtoConverter.toMc(data.equipment().mainHand()));
        this.setItemSlot(EquipmentSlot.OFFHAND, McDtoConverter.toMc(data.equipment().offHand()));
        this.setItemSlot(EquipmentSlot.FEET, McDtoConverter.toMc(data.equipment().feet()));
        this.setItemSlot(EquipmentSlot.CHEST, McDtoConverter.toMc(data.equipment().chest()));
        this.setItemSlot(EquipmentSlot.HEAD, McDtoConverter.toMc(data.equipment().head()));
        this.setItemSlot(EquipmentSlot.LEGS, McDtoConverter.toMc(data.equipment().legs()));

        if (data.isUsingItem()) {
            InteractionHand hand = McDtoConverter.toInteractionHand(data.activeHand());
            // アイテムの使用を開始した、または「使っている手」が切り替わった場合
            if (!this.isUsingItem() || this.getUsedItemHand() != hand) {
                this.startUsingItem(hand);

                // クライアント側のエンティティ（RemotePlayer）はバニラの仕様上、
                // startUsingItem内でEntityDataフラグが立たないため、手動で状態を上書き同期する
                this.setLivingEntityFlag(1, true); // アイテムを使用中
                this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND); // オフハンドを使用中か
            }
        } else {
            // アイテムの使用を停止した場合は、フラグと内部変数を確実にリセットする
            this.setLivingEntityFlag(1, false);
            this.setLivingEntityFlag(2, false);
            this.useItem = net.minecraft.world.item.ItemStack.EMPTY;
            this.useItemRemaining = 0;
        }

    }

    public String getGhostUuid() {
        return this.ghostUuid;
    }

    //? if >=26.3 {
    /*@Override
    protected InterpolationHandler createInterpolationHandler() {
        // 毎 tick 更新と相性の悪い SteppedInterpolationHandler の代わりに線形補間を使用する
        return LinearInterpolationHandler.create(this, INTERPOLATION_STEPS);
    }
    *///?}

    @Override
    public void tick() {
        super.tick();
        // 当 tick の実補間移動量から速度を算出し、マント揺れ等を自然に反映しつつ静止時は 0 に収束させる
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        double distSqr = dx * dx + dy * dy + dz * dz;
        if (distSqr > MAX_VELOCITY_DISTANCE_SQR) {
            this.setDeltaMovement(0.0, 0.0, 0.0);
        } else {
            this.setDeltaMovement(dx, dy, dz);
        }
    }

    // このエンティティはクライアントサイドのみの幻影なので、
    // 物理演算やAIのtick処理は一切行わないようにする

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