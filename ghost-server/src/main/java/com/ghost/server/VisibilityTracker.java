package com.ghost.server;

import com.ghost.api.dto.PlayerData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接続セッションの状態管理およびプレイヤー間の視認範囲・相互追跡（trackedPlayers）の整合性を担うクラス。
 * WebSocket 実装から切り離すことで、単体テストを容易にし、インスタンス間の状態汚染（static 共有）を排除します。
 *
 * @param <K> セッションを識別するキー型（本番では WebSocket）
 */
public class VisibilityTracker<K> {

    private final Map<K, GhostClientData> sessions = new ConcurrentHashMap<>();
    private final double visibleRangeSqr;

    public VisibilityTracker(double visibleRange) {
        if (visibleRange <= 0.0 || Double.isNaN(visibleRange) || Double.isInfinite(visibleRange)) {
            throw new IllegalArgumentException("visibleRange must be positive: " + visibleRange);
        }
        this.visibleRangeSqr = visibleRange * visibleRange;
    }

    /**
     * 未認証クライアントの接続を登録します。
     */
    public void registerConnection(K key, String nonce) {
        sessions.put(key, GhostClientData.unauthenticated(nonce));
    }

    /**
     * 指定キーのセッション情報を取得します。
     */
    public Optional<GhostClientData> getSession(K key) {
        return Optional.ofNullable(sessions.get(key));
    }

    /**
     * 指定セッションを認証済み状態へ遷移させます。
     */
    public boolean markAuthenticated(K key) {
        GhostClientData updated = sessions.computeIfPresent(key, (k, existing) -> GhostClientData.authenticated());
        return updated != null;
    }

    /**
     * プレイヤー状態更新の配信計画。
     *
     * @param notifyUpdateTargets  送信者の UPDATE を送るべき周囲ピア一覧
     * @param backfillToSender     新規に視認範囲へ入ったピアの最新データ（送信者へ送り返す）
     * @param notifyDespawnTargets 送信者の DESPAWN を送るべき範囲外ピア一覧
     * @param despawnToSender      範囲外になったピアの UUID 一覧（送信者へ DESPAWN 送信）
     */
    public record UpdateResult<K>(
            List<K> notifyUpdateTargets,
            List<PlayerData> backfillToSender,
            List<K> notifyDespawnTargets,
            List<String> despawnToSender
    ) {
        public static <K> UpdateResult<K> empty() {
            return new UpdateResult<>(List.of(), List.of(), List.of(), List.of());
        }
    }

    /**
     * 送信者の最新 {@link PlayerData} を登録・更新し、全認証済みピアとの対称的な視認追跡状態を更新します。
     * 初回の UPDATE 受信時でも即座に周囲プレイヤーと相互同期が行われます。
     */
    public UpdateResult<K> updatePlayer(K senderKey, PlayerData newData) {
        GhostClientData updatedSender = sessions.computeIfPresent(senderKey, (k, existing) -> {
            if (!existing.isAuthenticated()) {
                return existing;
            }
            return existing.withPlayerData(newData);
        });

        if (updatedSender == null || !updatedSender.isAuthenticated()) {
            return UpdateResult.empty();
        }

        String senderUuid = newData.uuid();
        List<K> notifyUpdateTargets = new ArrayList<>();
        List<PlayerData> backfillToSender = new ArrayList<>();
        List<K> notifyDespawnTargets = new ArrayList<>();
        List<String> despawnToSender = new ArrayList<>();

        for (var entry : sessions.entrySet()) {
            K peerKey = entry.getKey();
            if (senderKey.equals(peerKey)) {
                continue;
            }
            GhostClientData peerDataHolder = entry.getValue();
            if (!peerDataHolder.isAuthenticated() || peerDataHolder.playerData() == null) {
                continue;
            }

            PlayerData peerPlayer = peerDataHolder.playerData();
            String peerUuid = peerPlayer.uuid();

            if (isWithinRange(newData, peerPlayer)) {
                notifyUpdateTargets.add(peerKey);

                // 相互追跡セットを対称に更新し、どちらかが新規視認状態になった場合は送信者へ相手の状態を返す
                boolean peerAddedSender = peerDataHolder.trackedPlayers().add(senderUuid);
                boolean senderAddedPeer = updatedSender.trackedPlayers().add(peerUuid);
                if (peerAddedSender || senderAddedPeer) {
                    backfillToSender.add(peerPlayer);
                }
            } else {
                // 範囲外または別ディメンション: 双方向の追跡状態をクリーンに解除
                boolean peerWasTracking = peerDataHolder.trackedPlayers().remove(senderUuid);
                boolean senderWasTracking = updatedSender.trackedPlayers().remove(peerUuid);
                if (peerWasTracking) {
                    notifyDespawnTargets.add(peerKey);
                }
                if (senderWasTracking) {
                    despawnToSender.add(peerUuid);
                }
            }
        }

        return new UpdateResult<>(notifyUpdateTargets, backfillToSender, notifyDespawnTargets, despawnToSender);
    }

    /**
     * 切断処理の結果。
     *
     * @param disconnectedPlayer         切断したプレイヤーのデータ（未認証または未同期の場合は null）
     * @param authenticatedNotifyTargets LEAVE パケットを送信すべき認証済みピア一覧
     */
    public record RemoveResult<K>(
            @Nullable PlayerData disconnectedPlayer,
            List<K> authenticatedNotifyTargets
    ) {
    }

    /**
     * 接続を削除し、他ピアの追跡セットから切断済みプレイヤーの UUID を除去した上で
     * LEAVE 通知対象（認証済みクライアントのみ）を返します。
     */
    public RemoveResult<K> removeConnection(K key) {
        GhostClientData removed = sessions.remove(key);
        if (removed == null || removed.playerData() == null) {
            return new RemoveResult<>(null, List.of());
        }

        PlayerData disconnectedPlayer = removed.playerData();
        String disconnectedUuid = disconnectedPlayer.uuid();
        List<K> authenticatedTargets = new ArrayList<>();

        for (var entry : sessions.entrySet()) {
            GhostClientData peer = entry.getValue();
            peer.trackedPlayers().remove(disconnectedUuid);
            if (peer.isAuthenticated()) {
                authenticatedTargets.add(entry.getKey());
            }
        }
        return new RemoveResult<>(disconnectedPlayer, authenticatedTargets);
    }

    /**
     * 2 人のプレイヤーが同一ディメンションかつ視認距離内にいるかを判定します。
     */
    public boolean isWithinRange(PlayerData pl1, PlayerData pl2) {
        double diffX = pl1.pos().x() - pl2.pos().x();
        double diffZ = pl1.pos().z() - pl2.pos().z();
        double horizontalDistanceSqr = diffX * diffX + diffZ * diffZ;
        return pl1.dimension().equals(pl2.dimension())
                && horizontalDistanceSqr < visibleRangeSqr;
    }

    /**
     * 現在の登録セッション数を返します。
     */
    public int sessionCount() {
        return sessions.size();
    }
}
