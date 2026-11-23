package com.ghost.api.dto;

/**
 * プレイヤーの同期状態を転送するための不変データレコード。
 * Minecraft ライブラリ非依存であり、全モジュール共通の契約として利用されます。
 *
 * @param pos         プレイヤーの 3D 座標
 * @param rot         プレイヤーの回転角（x=Pitch, y=Yaw）
 * @param uuid        プレイヤーの一意識別子（UUID 文字列）
 * @param name        プレイヤーの表示名
 * @param pose        現在のポーズ名（例: "STANDING"）
 * @param dimension   現在のディメンション ID（例: "minecraft:overworld"）
 * @param skinParts   表示中のスキンパーツビットマスク
 * @param mainArm     利き腕（"RIGHT" または "LEFT"）
 * @param swingTime   腕振りアニメーションの経過 tick
 * @param equipment   装備情報（6 スロット）
 * @param isUsingItem アイテムを使用中かどうか
 * @param activeHand  アイテム使用中の手（"MAIN_HAND" または "OFF_HAND"）
 */
public record PlayerData(
        Vec3Dto pos,
        Vec2Dto rot,
        String uuid,
        String name,
        String pose,
        String dimension,
        byte skinParts,
        String mainArm,
        int swingTime,
        EquipmentDto equipment,
        boolean isUsingItem,
        String activeHand
) {
    public PlayerData {
        pos = pos != null ? pos : Vec3Dto.ZERO;
        rot = rot != null ? rot : Vec2Dto.ZERO;
        uuid = uuid != null ? uuid : "";
        name = name != null ? name : "";
        pose = pose != null ? pose : "STANDING";
        dimension = dimension != null ? dimension : "";
        mainArm = mainArm != null ? mainArm : "RIGHT";
        equipment = equipment != null ? equipment : EquipmentDto.EMPTY;
        activeHand = activeHand != null ? activeHand : "MAIN_HAND";
    }

    /**
     * デフォルト値で初期化するコンストラクタ。
     */
    public PlayerData() {
        this(Vec3Dto.ZERO, Vec2Dto.ZERO, "", "", "STANDING", "", (byte) 127, "RIGHT", 0,
                EquipmentDto.EMPTY, false, "MAIN_HAND");
    }
}