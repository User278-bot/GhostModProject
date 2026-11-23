package com.ghost.server;

import com.ghost.api.dto.EquipmentDto;
import com.ghost.api.dto.PlayerData;
import com.ghost.api.dto.Vec2Dto;
import com.ghost.api.dto.Vec3Dto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link VisibilityTracker} の視認範囲判定・対称的相互追跡・切断時クリーンアップを検証するユニットテスト。
 */
class VisibilityTrackerTest {

    private static PlayerData playerAt(String uuid, String name, double x, double z, String dimension) {
        return new PlayerData(
                new Vec3Dto(x, 64.0, z),
                Vec2Dto.ZERO,
                uuid,
                name,
                "STANDING",
                dimension,
                (byte) 127,
                "RIGHT",
                0,
                EquipmentDto.EMPTY,
                false,
                "MAIN_HAND"
        );
    }

    @Test
    void firstUpdateWithinRange_PerformsImmediateMutualSync() {
        VisibilityTracker<String> tracker = new VisibilityTracker<>(50.0);
        tracker.registerConnection("session-a", "nonce-a");
        tracker.registerConnection("session-b", "nonce-b");
        // 登録セッションの認証遷移は true、未登録セッションは false を返すこと
        assertTrue(tracker.markAuthenticated("session-a"));
        assertTrue(tracker.markAuthenticated("session-b"));
        assertFalse(tracker.markAuthenticated("unknown-session"));

        // A の初回 UPDATE（まだ B は PlayerData 未登録）
        PlayerData dataA = playerAt("uuid-a", "Alice", 0, 0, "minecraft:overworld");
        var resA = tracker.updatePlayer("session-a", dataA);
        assertTrue(resA.notifyUpdateTargets().isEmpty());
        assertTrue(resA.backfillToSender().isEmpty());

        // B の初回 UPDATE（A の視認範囲内、X/Z 双方オフセットあり）→ A への配信と、B への A バックフィルが即座に発生すること
        PlayerData dataB = playerAt("uuid-b", "Bob", 10, 15, "minecraft:overworld");
        var resB = tracker.updatePlayer("session-b", dataB);
        assertEquals(List.of("session-a"), resB.notifyUpdateTargets());
        assertEquals(List.of(dataA), resB.backfillToSender());
        assertTrue(resB.notifyDespawnTargets().isEmpty());
        assertTrue(resB.despawnToSender().isEmpty());

        // 双方の trackedPlayers が対称に保持されていること
        assertTrue(tracker.getSession("session-a").orElseThrow().trackedPlayers().contains("uuid-b"));
        assertTrue(tracker.getSession("session-b").orElseThrow().trackedPlayers().contains("uuid-a"));

        // 2 回目の範囲内 UPDATE ではバックフィルは重複発生しないこと
        var resB2 = tracker.updatePlayer("session-b", playerAt("uuid-b", "Bob", 12, 15, "minecraft:overworld"));
        assertEquals(List.of("session-a"), resB2.notifyUpdateTargets());
        assertTrue(resB2.backfillToSender().isEmpty());
    }

    @Test
    void movingOutOfRangeAndAcrossDimensions_EmitsSymmetricDespawnAndResyncsOnReturn() {
        VisibilityTracker<String> tracker = new VisibilityTracker<>(30.0);
        tracker.registerConnection("a", "n1");
        tracker.registerConnection("b", "n2");
        assertTrue(tracker.markAuthenticated("a"));
        assertTrue(tracker.markAuthenticated("b"));

        PlayerData dataA = playerAt("uuid-a", "Alice", 0, 0, "minecraft:overworld");
        tracker.updatePlayer("a", dataA);
        tracker.updatePlayer("b", playerAt("uuid-b", "Bob", 5, 5, "minecraft:overworld"));

        // B が範囲外へ移動 → 双方向に DESPAWN が発生し trackedPlayers から除去されること
        var outRes = tracker.updatePlayer("b", playerAt("uuid-b", "Bob", 100, -40, "minecraft:overworld"));
        assertTrue(outRes.notifyUpdateTargets().isEmpty());
        assertEquals(List.of("a"), outRes.notifyDespawnTargets());
        assertEquals(List.of("uuid-a"), outRes.despawnToSender());
        assertFalse(tracker.getSession("a").orElseThrow().trackedPlayers().contains("uuid-b"));
        assertFalse(tracker.getSession("b").orElseThrow().trackedPlayers().contains("uuid-a"));

        // 再び範囲内へ戻ると再同期されること
        var returnRes = tracker.updatePlayer("b", playerAt("uuid-b", "Bob", 10, 10, "minecraft:overworld"));
        assertEquals(List.of("a"), returnRes.notifyUpdateTargets());
        assertEquals(List.of(dataA), returnRes.backfillToSender());

        // 別ディメンションへ移動 → 座標が近くても DESPAWN になること
        var dimRes = tracker.updatePlayer("b", playerAt("uuid-b", "Bob", 10, 10, "minecraft:the_nether"));
        assertTrue(dimRes.notifyUpdateTargets().isEmpty());
        assertEquals(List.of("a"), dimRes.notifyDespawnTargets());
        assertEquals(List.of("uuid-a"), dimRes.despawnToSender());
    }

    @Test
    void removeConnection_CleansPeerTrackedSetsAndNotifiesOnlyAuthenticatedPeers() {
        VisibilityTracker<String> tracker = new VisibilityTracker<>(100.0);
        tracker.registerConnection("a", "n1");
        tracker.registerConnection("b", "n2");
        tracker.registerConnection("unauth", "n3");
        tracker.markAuthenticated("a");
        tracker.markAuthenticated("b");

        PlayerData dataA = playerAt("uuid-a", "Alice", 0, 0, "minecraft:overworld");
        tracker.updatePlayer("a", dataA);
        tracker.updatePlayer("b", playerAt("uuid-b", "Bob", 5, 0, "minecraft:overworld"));

        // A が切断 → B の trackedPlayers から uuid-a が除去され、認証済み B のみに通知対象が絞られること
        var removeRes = tracker.removeConnection("a");
        assertEquals(dataA, removeRes.disconnectedPlayer());
        assertEquals(List.of("b"), removeRes.authenticatedNotifyTargets());
        assertFalse(tracker.getSession("b").orElseThrow().trackedPlayers().contains("uuid-a"));
        assertEquals(2, tracker.sessionCount());
    }

    @Test
    void unauthenticatedSessionUpdate_IsIgnored() {
        VisibilityTracker<String> tracker = new VisibilityTracker<>(100.0);
        tracker.registerConnection("unauth", "nonce");

        var res = tracker.updatePlayer("unauth", playerAt("u", "Unauth", 0, 0, "minecraft:overworld"));
        assertTrue(res.notifyUpdateTargets().isEmpty());
        assertTrue(res.backfillToSender().isEmpty());
        assertNull(tracker.getSession("unauth").orElseThrow().playerData());
    }
}
