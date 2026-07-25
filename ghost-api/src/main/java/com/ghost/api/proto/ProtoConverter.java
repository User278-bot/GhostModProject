package com.ghost.api.proto;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import com.ghost.api.dto.item.ItemDto;
import com.ghost.api.dto.item.components.CustomModelDataDto;
import com.ghost.api.dto.item.components.TrimDto;
import com.ghost.api.packet.MessageType;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * DTO クラスと protobuf 生成クラス間の相互変換を行うユーティリティ。
 * <p>
 * 認証パケット（AUTH_*）は対象外で JSON テキストのまま送信される。
 * このクラスは UPDATE / LEAVE / DESPAWN パケットのシリアライズに使用する。
 */
public final class ProtoConverter {

    // GhostPacketProto.type に使用する ordinal 値（MessageType と対応）
    public static final int TYPE_UPDATE = 0;
    public static final int TYPE_JOIN = 1;
    public static final int TYPE_LEAVE = 2;
    public static final int TYPE_DESPAWN = 3;

    private ProtoConverter() {
    }

    // ============================================================
    // PlayerData のシリアライズ（UPDATE / JOIN パケット用）
    // ============================================================

    /**
     * PlayerData DTO を UPDATE パケットの byte[] にシリアライズする。
     *
     * @param data 送信対象の PlayerData
     * @return シリアライズ済み GhostPacketProto の byte 配列
     */
    public static byte[] serializeUpdatePacket(PlayerData data) {
        PlayerDataProto proto = toProto(data);
        GhostPacketProto packet = GhostPacketProto.newBuilder()
                .setType(TYPE_UPDATE)
                .setPayload(proto.toByteString())
                .build();
        return packet.toByteArray();
    }

    /**
     * UPDATE パケットの byte[] から PlayerData DTO にデシリアライズする。
     *
     * @param bytes シリアライズ済み GhostPacketProto の byte 配列
     * @return 変換後の PlayerData
     * @throws InvalidProtocolBufferException 不正なバイト列の場合
     */
    public static PlayerData deserializeUpdatePacket(byte[] bytes) throws InvalidProtocolBufferException {
        GhostPacketProto packet = GhostPacketProto.parseFrom(bytes);
        PlayerDataProto proto = PlayerDataProto.parseFrom(packet.getPayload());
        return fromProto(proto);
    }

    // ============================================================
    // UUID のシリアライズ（LEAVE / DESPAWN パケット用）
    // ============================================================

    /**
     * UUID 文字列を LEAVE または DESPAWN パケットの byte[] にシリアライズする。
     *
     * @param type MessageType.LEAVE または MessageType.DESPAWN
     * @param uuid プレイヤーの UUID 文字列
     * @return シリアライズ済み GhostPacketProto の byte 配列
     */
    public static byte[] serializeUuidPacket(MessageType type, String uuid) {
        int typeInt = (type == MessageType.LEAVE) ? TYPE_LEAVE : TYPE_DESPAWN;
        GhostPacketProto packet = GhostPacketProto.newBuilder()
                .setType(typeInt)
                .setPayload(com.google.protobuf.ByteString.copyFromUtf8(uuid))
                .build();
        return packet.toByteArray();
    }

    // ============================================================
    // パケットタイプの識別
    // ============================================================

    /**
     * byte[] からパケットタイプのみを取得する。
     *
     * @param bytes シリアライズ済み GhostPacketProto の byte 配列
     * @return 対応する MessageType（不明な場合は UNRECOGNIZED）
     * @throws InvalidProtocolBufferException 不正なバイト列の場合
     */
    public static MessageType deserializePacketType(byte[] bytes) throws InvalidProtocolBufferException {
        GhostPacketProto packet = GhostPacketProto.parseFrom(bytes);
        return switch (packet.getType()) {
            case TYPE_UPDATE -> MessageType.UPDATE;
            case TYPE_JOIN -> MessageType.JOIN;
            case TYPE_LEAVE -> MessageType.LEAVE;
            case TYPE_DESPAWN -> MessageType.DESPAWN;
            default -> MessageType.UNRECOGNIZED;
        };
    }

    /**
     * LEAVE / DESPAWN パケットから UUID 文字列を取得する。
     *
     * @param bytes シリアライズ済み GhostPacketProto の byte 配列
     * @return UUID 文字列
     * @throws InvalidProtocolBufferException 不正なバイト列の場合
     */
    public static String deserializeUuid(byte[] bytes) throws InvalidProtocolBufferException {
        GhostPacketProto packet = GhostPacketProto.parseFrom(bytes);
        return packet.getPayload().toStringUtf8();
    }

    // ============================================================
    // 内部変換メソッド: DTO → protobuf
    // ============================================================

    /**
     * PlayerData DTO を PlayerDataProto に変換する。
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

        // null の場合はフィールドを省略（optional field）
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
    // 内部変換メソッド: protobuf → DTO
    // ============================================================

    /**
     * PlayerDataProto を PlayerData DTO に変換する。
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
                cmd.getColorsList()
        );
    }

    private static TrimDto fromProto(Trim t) {
        return new TrimDto(t.getPattern(), t.getMaterial());
    }
}
