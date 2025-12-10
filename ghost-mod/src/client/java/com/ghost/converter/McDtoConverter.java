package com.ghost.converter;

import com.ghost.api.dto.Vec3Dto;
import com.ghost.api.dto.Vec2Dto;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;

@SuppressWarnings("unused")
public final class McDtoConverter {
    private McDtoConverter() {
    }

    public static Vec3Dto fromMc(Vec3 vec3) {
        return new Vec3Dto(vec3.x, vec3.y, vec3.z);
    }

    public static String fromMc(InteractionHand hand) {
        return hand != null ? hand.name() : InteractionHand.MAIN_HAND.name();
    }

    public static Vec2Dto fromMc(Vec2 vec2) {
        return new Vec2Dto(vec2.x, vec2.y);
    }

    public static Vec3 toMc(Vec3Dto v3d) {
        return new Vec3(v3d.x(), v3d.y(), v3d.z());
    }

    public static Vec2 toMc(Vec2Dto v2d) {
        return new Vec2(v2d.x(), v2d.y());
    }
}
