package com.ghost.api.proto;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.item.components.CustomModelDataDto;
import com.ghost.api.dto.item.components.TrimDto;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Objects;

/**
 * DTO クラスと Protobuf 生成クラス間の相互変換およびパケットのエンコード／デコードを行うユーティリティ。
 * 認証系・同期系すべてのパケットを {@link GhostPacketProto} の {@code oneof} バイナリ形式で統一します。
 */
public final class ProtoConverter {

    /** 現行通信プロトコルバージョン（非互換変更時にインクリメントする） */
    public static final int PROTOCOL_VERSION = 1;

    private ProtoConverter() {
    }

    // ============================================================
    // パケット エンコード（DTO / 認証情報 → byte[]）
    // ============================================================

    /**
     * PlayerData DTO を UPDATE パケットのバイト列にエンコードします。
     */
    public static byte[] encodeUpdate(PlayerData data) {
        Objects.requireNonNull(data, "data must not be null");
        return GhostPacketProto.newBuilder()
                .setUpdate(toProto(data))
                .build()
                .toByteArray();
    }

    /**
     * 切断通知（LEAVE）パケットのバイト列にエンコードします。
     */
    public static byte[] encodeLeave(String uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return GhostPacketProto.newBuilder()
                .setLeaveUuid(uuid)
                .build()
                .toByteArray();
    }

    /**
     * 視認範囲外（DESPAWN）パケットのバイト列にエンコードします。
     */
    public static byte[] encodeDespawn(String uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return GhostPacketProto.newBuilder()
                .setDespawnUuid(uuid)
                .build()
                .toByteArray();
    }

    /**
     * CHAP 認証チャレンジパケットを現行プロトコルバージョンでエンコードします。
     */
    public static byte[] encodeAuthChallenge(String nonce) {
        return encodeAuthChallenge(nonce, PROTOCOL_VERSION);
    }

    /**
     * CHAP 認証チャレンジパケットを指定プロトコルバージョンでエンコードします。
     */
    public static byte[] encodeAuthChallenge(String nonce, int protocolVersion) {
        Objects.requireNonNull(nonce, "nonce must not be null");
        return GhostPacketProto.newBuilder()
                .setAuthChallenge(AuthChallenge.newBuilder()
                        .setNonce(nonce)
                        .setProtocolVersion(protocolVersion)
                        .build())
                .build()
                .toByteArray();
    }

    /**
     * CHAP 認証レスポンスパケットを現行プロトコルバージョンでエンコードします。
     */
    public static byte[] encodeAuthResponse(String hash) {
        return encodeAuthResponse(hash, PROTOCOL_VERSION);
    }

    /**
     * CHAP 認証レスポンスパケットを指定プロトコルバージョンでエンコードします。
     */
    public static byte[] encodeAuthResponse(String hash, int protocolVersion) {
        Objects.requireNonNull(hash, "hash must not be null");
        return GhostPacketProto.newBuilder()
                .setAuthResponse(AuthResponse.newBuilder()
                        .setHash(hash)
                        .setProtocolVersion(protocolVersion)
                        .build())
                .build()
                .toByteArray();
    }

    /**
     * CHAP 認証成功パケットをエンコードします。
     */
    public static byte[] encodeAuthSuccess() {
        return GhostPacketProto.newBuilder()
                .setAuthSuccess(AuthSuccess.getDefaultInstance())
                .build()
                .toByteArray();
    }

    // ============================================================
    // パケット デコード（byte[] → GhostPacketProto 1パス変換）
    // ============================================================

    /**
     * バイト列を 1 パスで {@link GhostPacketProto} にデコードします。
     * 呼び出し側は {@link GhostPacketProto#getPayloadCase()} で種別を判別できます。
     */
    public static GhostPacketProto decode(byte[] bytes) throws InvalidProtocolBufferException {
        return GhostPacketProto.parseFrom(bytes);
    }

    // ============================================================
    // 内部変換メソッド: DTO → Protobuf
    // ============================================================

    /**
     * PlayerData DTO を PlayerDataProto に変換します。
     */
    public static PlayerDataProto toProto(PlayerData data) {
        return PlayerDataProto.newBuilder()
                .setPos(toProto(data.pos()))
                .setRot(toProto(data.rot()))
                .setUuid(data.uuid())
                .setName(data.name())
                .setPose(data.pose())
                .setDimension(data.dimension())
                .setSkinParts(data.skinParts())
                .setMainArm(data.mainArm())
                .setSwingTime(data.swingTime())
                .setEquipment(toProto(data.equipment()))
                .setIsUsingItem(data.isUsingItem())
                .setActiveHand(data.activeHand())
                .build();
    }

    private static Vec3 toProto(Vec3Dto v) {
        return Vec3.newBuilder()
                .setX(v.x())
                .setY(v.y())
                .setZ(v.z())
                .build();
    }

    private static Vec2 toProto(Vec2Dto v) {
        return Vec2.newBuilder()
                .setX(v.x())
                .setY(v.y())
                .build();
    }

    private static Equipment toProto(EquipmentDto e) {
        return Equipment.newBuilder()
                .setMainHand(toProto(e.mainHand()))
                .setOffHand(toProto(e.offHand()))
                .setHead(toProto(e.head()))
                .setChest(toProto(e.chest()))
                .setLegs(toProto(e.legs()))
                .setFeet(toProto(e.feet()))
                .build();
    }

    private static Item toProto(ItemDto item) {
        Item.Builder builder = Item.newBuilder()
                .setId(item.id())
                .setDamage(item.damage())
                .setHasGlint(item.hasGlint())
                .setColor(item.color())
                .setItemModel(item.itemModel() != null ? item.itemModel() : "");

        // null の場合は optional フィールドを省略
        if (item.customModelData() != null) {
            builder.setCustomModelData(toProto(item.customModelData()));
        }
        if (item.trim() != null) {
            builder.setTrim(toProto(item.trim()));
        }
        return builder.build();
    }

    private static CustomModelData toProto(CustomModelDataDto cmd) {
        return CustomModelData.newBuilder()
                .addAllFloats(cmd.floats())
                .addAllFlags(cmd.flags())
                .addAllStrings(cmd.strings())
                .addAllColors(cmd.colors())
                .setModel(cmd.model())
                .build();
    }

    private static Trim toProto(TrimDto t) {
        return Trim.newBuilder()
                .setMaterial(t.material())
                .setPattern(t.pattern())
                .build();
    }

    // ============================================================
    // 内部変換メソッド: Protobuf → DTO
    // ============================================================

    /**
     * PlayerDataProto を PlayerData DTO に変換します。
     */
    public static PlayerData fromProto(PlayerDataProto proto) {
        return new PlayerData(
                fromProto(proto.getPos()),
                fromProto(proto.getRot()),
                proto.getUuid(),
                proto.getName(),
                proto.getPose(),
                proto.getDimension(),
                (byte) proto.getSkinParts(),
                proto.getMainArm(),
                proto.getSwingTime(),
                fromProto(proto.getEquipment()),
                proto.getIsUsingItem(),
                proto.getActiveHand()
        );
    }

    private static Vec3Dto fromProto(Vec3 v) {
        return new Vec3Dto(v.getX(), v.getY(), v.getZ());
    }

    private static Vec2Dto fromProto(Vec2 v) {
        return new Vec2Dto(v.getX(), v.getY());
    }

    private static EquipmentDto fromProto(Equipment e) {
        return new EquipmentDto(
                fromProto(e.getMainHand()),
                fromProto(e.getOffHand()),
                fromProto(e.getHead()),
                fromProto(e.getChest()),
                fromProto(e.getLegs()),
                fromProto(e.getFeet())
        );
    }

    private static ItemDto fromProto(Item item) {
        ItemDto.Builder builder = new ItemDto.Builder(item.getId())
                .damage(item.getDamage())
                .hasGlint(item.getHasGlint())
                .color(item.getColor())
                .itemModel(item.getItemModel());

        // optional フィールド: hasFoo() で存在チェック
        if (item.hasCustomModelData()) {
            builder.customModelData(fromProto(item.getCustomModelData()));
        }
        if (item.hasTrim()) {
            builder.trim(fromProto(item.getTrim()));
        }
        return builder.build();
    }

    private static CustomModelDataDto fromProto(CustomModelData cmd) {
        return new CustomModelDataDto(
                cmd.getFloatsList(),
                cmd.getFlagsList(),
                cmd.getStringsList(),
                cmd.getColorsList(),
                cmd.getModel()
        );
    }

    private static TrimDto fromProto(Trim t) {
        return new TrimDto(t.getPattern(), t.getMaterial());
    }
}
