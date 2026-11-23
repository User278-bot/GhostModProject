package com.ghost.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.player.PlayerSkin;
*///?} elif >=1.20.6 {
/*import net.minecraft.client.resources.PlayerSkin;
*///?} else {
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.resources.ResourceLocation;
//?}

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ゴーストプレイヤーのスキン非同期取得と UUID 単位のキャッシュを管理するクラス（Issue #68 対応）。
 * 同一プレイヤーの視認範囲出入り・再スポーン時に Mojang Session API を繰り返し呼び出すのを防ぎ、
 * 取得失敗時はキャッシュを汚染せず次回リトライ可能にします。
 */
public final class GhostSkinCache {

    //? if >=1.20.6 {
    /*private static final ConcurrentHashMap<UUID, CompletableFuture<Optional<PlayerSkin>>> CACHE =
            new ConcurrentHashMap<>();
    *///?} else {
    private static final ConcurrentHashMap<UUID, CompletableFuture<Optional<ResourceLocation>>> CACHE =
            new ConcurrentHashMap<>();
    //?}

    private GhostSkinCache() {
    }

    /**
     * 指定プロフィールのスキンをキャッシュから取得、または非同期でフェッチします。
     */
    //? if >=1.20.6 {
    /*public static CompletableFuture<Optional<PlayerSkin>> getOrFetch(GameProfile profile) {
    *///?} else {
    public static CompletableFuture<Optional<ResourceLocation>> getOrFetch(GameProfile profile) {
    //?}
        if (profile == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        //? if >=1.21.11 {
        /*UUID playerId = profile.id();
        *///?} else {
        UUID playerId = profile.getId();
        //?}
        if (playerId == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        //? if >=1.20.6 {
        /*CompletableFuture<Optional<PlayerSkin>> future = CACHE.computeIfAbsent(playerId, id -> fetchAsync(profile, id));
        *///?} else {
        CompletableFuture<Optional<ResourceLocation>> future = CACHE.computeIfAbsent(playerId, id -> fetchAsync(profile, id));
        //?}

        // 取得失敗または空結果の場合はキャッシュから取り除き、次回スポーン時にリトライできるようにする
        future.whenComplete((result, error) -> {
            if (error != null || result.isEmpty()) {
                CACHE.remove(playerId, future);
            }
        });
        return future;
    }

    /**
     * スキンキャッシュを全消去します（ワールド切断時などに呼び出します）。
     */
    public static void clear() {
        CACHE.clear();
    }

    //? if >=1.21.11 {
    /*private static CompletableFuture<Optional<PlayerSkin>> fetchAsync(GameProfile profile, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var fetched = Minecraft.getInstance().services().sessionService().fetchProfile(playerId, true);
                return fetched != null ? fetched.profile() : null;
            } catch (Exception e) {
                LogUtils.getLogger().debug("Failed to fetch profile for skin cache: {}", playerId, e);
                return null;
            }
        }).thenCompose(filledProfile -> {
            if (filledProfile == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return Minecraft.getInstance().getSkinManager().get(filledProfile)
                    .exceptionally(ex -> {
                        LogUtils.getLogger().debug("Failed to load skin for {}", playerId, ex);
                        return Optional.empty();
                    });
        });
    }
    *///?} elif >=1.21.4 {
    /*private static CompletableFuture<Optional<PlayerSkin>> fetchAsync(GameProfile profile, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var fetched = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(playerId, true);
                return fetched != null ? fetched.profile() : null;
            } catch (Exception e) {
                LogUtils.getLogger().debug("Failed to fetch profile for skin cache: {}", playerId, e);
                return null;
            }
        }).thenCompose(filledProfile -> {
            if (filledProfile == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return Minecraft.getInstance().getSkinManager().getOrLoad(filledProfile)
                    .exceptionally(ex -> {
                        LogUtils.getLogger().debug("Failed to load skin for {}", playerId, ex);
                        return Optional.empty();
                    });
        });
    }
    *///?} elif >=1.20.6 {
    /*private static CompletableFuture<Optional<PlayerSkin>> fetchAsync(GameProfile profile, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var fetched = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(playerId, true);
                return fetched != null ? fetched.profile() : null;
            } catch (Exception e) {
                LogUtils.getLogger().debug("Failed to fetch profile for skin cache: {}", playerId, e);
                return null;
            }
        }).thenCompose(filledProfile -> {
            if (filledProfile == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return Minecraft.getInstance().getSkinManager().getOrLoad(filledProfile)
                    .thenApply(Optional::ofNullable)
                    .exceptionally(ex -> {
                        LogUtils.getLogger().debug("Failed to load skin for {}", playerId, ex);
                        return Optional.empty();
                    });
        });
    }
    *///?} else {
    private static CompletableFuture<Optional<ResourceLocation>> fetchAsync(GameProfile profile, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Minecraft.getInstance().getMinecraftSessionService().fillProfileProperties(profile, true);
            } catch (Exception e) {
                LogUtils.getLogger().debug("Failed to fill profile properties for skin cache: {}", playerId, e);
                return null;
            }
        }).thenCompose(filledProfile -> {
            if (filledProfile == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            CompletableFuture<Optional<ResourceLocation>> skinFuture = new CompletableFuture<>();
            Minecraft.getInstance().execute(() -> {
                try {
                    Minecraft.getInstance().getSkinManager().registerSkins(
                            filledProfile,
                            (type, location, texture) -> {
                                if (type == MinecraftProfileTexture.Type.SKIN) {
                                    skinFuture.complete(Optional.ofNullable(location));
                                }
                            },
                            true);
                } catch (Exception e) {
                    LogUtils.getLogger().debug("Failed to register skins for {}", playerId, e);
                    skinFuture.complete(Optional.empty());
                }
            });
            return skinFuture;
        });
    }
    //?}
}
